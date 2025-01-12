package ru.practicum.ewm.user.service;

import ru.practicum.ewm.user.dto.NewUserDto;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;

import java.util.List;

public interface UserService {

    UserDto addNewUser(NewUserDto request);

    List<UserDto> getUsers(List<Integer> ids, Integer from, Integer size);

    User findById(Integer userId);

    void deleteUserById(Integer id);

}