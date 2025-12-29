package com.chep.demo.todo.service.auth;

import com.chep.demo.todo.domain.project.Project;
import com.chep.demo.todo.domain.project.ProjectRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceMemberRepository;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.exception.auth.AuthenticationException;
import com.chep.demo.todo.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ProjectRepository projectRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public AuthResult register(String email, String password, String name) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email address is already registered.");
        }

        // password 해시
        String encodedPassword = passwordEncoder.encode(password);

        // user 생성
        User user = User.builder()
                .name(name)
                .email(email)
                .password(encodedPassword)
                .build();

        User saved = userRepository.save(user);
        createPersonalWorkspace(saved);
        String accessToken = jwtTokenProvider.generateAccessToken(saved.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(saved.getId());

        return new AuthResult(user, accessToken, refreshToken);
    }

    public AuthResult login(String email, String rawPassword) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 4. AuthResult 반환
        return new AuthResult(user, accessToken, refreshToken);

    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
    }

    public AuthResult refresh(String refreshToken) {
        // 1. refreshToken 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // 2. userId 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 3. 유저 조회
        User user = getUserById(userId);

        // 4. 새 accessToken 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId());

        return new AuthResult(user, newAccessToken, refreshToken);
    }

    private void createPersonalWorkspace(User owner) {
        Workspace workspace = Workspace.personal(owner);
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        workspaceMemberRepository.save(WorkspaceMember.owner(savedWorkspace, owner));
        projectRepository.save(Project.defaultProject(savedWorkspace, owner));
    }
}
