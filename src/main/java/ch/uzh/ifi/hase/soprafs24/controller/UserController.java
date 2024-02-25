package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.Permissions;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.NotFoundException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.lang.model.type.NullType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getAllUsers(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String authToken) {
        // if not authorized
        if (!userService.isAuthorized(authToken, Permissions.READ)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden action");
        }

        // fetch all users in the internal representation
        List<User> users = userService.getUsers();
        List<UserGetDTO> userGetDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
        }
        return userGetDTOs;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {

        // no auth needed to create user (this would actually be bad security design...)

        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // create user
        User createdUser = userService.createUser(userInput);
        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    }

    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getUserWithId(@PathVariable Long id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String authToken) {
        // if not authorized
        if (!userService.isAuthorized(authToken, Permissions.READ)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden action");
        }

        try {
            // need to return UserGetDTO, just User does not work
            User user = userService.getUserById(id);
            return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
        }
        catch (NotFoundException e) {
            // user id does not exist
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
        }
        catch (Exception e) {  // general catch
            // unexpected error
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error", e);
        }
    }

    /* Login, returns token */
    @PostMapping("/users/auth")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO authenticateUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        User getUser;

        // userInput has either username, password
        // todo password
        if (!Objects.equals(userInput.getUsername(), "") /* userInput.getPassword() != "" */) {
            getUser = userService.isUserAuthorized(userInput.getUsername(), userInput.getUsername());
        }
        // or user input has a token
        else if (!Objects.equals(userInput.getToken(), "")) {
            if (userService.isAuthorized(userInput.getToken(), Permissions.READ)) {
                getUser = userService.getUserByToken(userInput.getToken());
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either pass username,password or pass token.");
        }

        // return user object
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(getUser);
    }
}
