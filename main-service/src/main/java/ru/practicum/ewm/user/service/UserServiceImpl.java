package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.error.exceptions.EntityExistsException;
import ru.practicum.ewm.error.exceptions.NotFoundException;
import ru.practicum.ewm.user.converter.UsersConverter;
import ru.practicum.ewm.user.dto.NewUserDto;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UsersRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UsersConverter usersConverter;
    private final UsersRepository usersRepository;

    @Override
    public UserDto addNewUser(NewUserDto request) {
        if (usersRepository.findByEmailIgnoreCase(request.getEmail().toUpperCase()).isPresent()) {
            log.error("Создание нового пользователя отклонено: email={} занят", request.getEmail());
            throw new EntityExistsException(String.format("Данный email уже занят другим пользователем: %s", request.getEmail()));
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        usersRepository.save(user);
        log.info("Создан новый пользователь с id={}: {}", user.getId(), user);

        return usersConverter.toUserDto(user);
    }

    @Override
    public List<UserDto> getUsers(List<Integer> ids, Integer from, Integer size) {
        if (ids == null || ids.isEmpty()) {
            return usersRepository.findAll(PageRequest.of(from, size))
                    .stream()
                    .map(usersConverter::toUserDto)
                    .toList();
        }

        return usersRepository.findAllByIdIn(ids, PageRequest.of(from, size))
                .stream()
                .map(usersConverter::toUserDto)
                .toList();
    }

    @Override
    public User findById(Integer userId) {
        return usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId))
        );
    }

    @Override
    public void deleteUserById(Integer id) {
        User user = usersRepository.findById(id).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", id))
        );

        usersRepository.delete(user);
        log.info("Удалён пользователь с id={}", id);
    }
}