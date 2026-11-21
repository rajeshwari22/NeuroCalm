package com.neurocalm.service;

import com.neurocalm.dto.UserDTO;
import com.neurocalm.exception.ResourceNotFoundException;
import com.neurocalm.model.User;
import com.neurocalm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDTO createUser(UserDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("A user with this email already exists");
        }
        User saved = userRepository.save(new User(request.name(), request.email()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDTO getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return toDto(user);
    }

    private UserDTO toDto(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
