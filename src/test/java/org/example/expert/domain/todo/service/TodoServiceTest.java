package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("Todo 저장 성공")
    void saveTodo_Success() {
        // given
        AuthUser authUser = new AuthUser(1L, "test@test.com", UserRole.USER);
        TodoSaveRequest request = new TodoSaveRequest("Test Title", "Test Contents");
        String weather = "Sunny";

        User user = User.fromAuthUser(authUser);
        Todo savedTodo = new Todo("Test Title", "Test Contents", weather, user);

        given(weatherClient.getTodayWeather()).willReturn(weather);
        given(todoRepository.save(any(Todo.class))).willReturn(savedTodo);

        // when
        TodoSaveResponse response = todoService.saveTodo(authUser, request);

        // then
        assertNotNull(response);
        assertEquals("Test Title", response.getTitle());
        assertEquals("Test Contents", response.getContents());
        assertEquals(weather, response.getWeather());
        verify(weatherClient).getTodayWeather();
        verify(todoRepository).save(any(Todo.class));
    }

    @Test
    @DisplayName("Todo 목록 조회 성공")
    void getTodos_Success() {
        // given
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);

        User user = new User("test@test.com", "password", UserRole.USER);
        Todo todo1 = new Todo("Title 1", "Contents 1", "Sunny", user);
        Todo todo2 = new Todo("Title 2", "Contents 2", "Rainy", user);

        Page<Todo> todoPage = new PageImpl<>(List.of(todo1, todo2), pageable, 2);

        given(todoRepository.findAllByOrderByModifiedAtDesc(pageable)).willReturn(todoPage);

        // when
        Page<TodoResponse> result = todoService.getTodos(page, size);

        // then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("Title 1", result.getContent().get(0).getTitle());
        assertEquals("Title 2", result.getContent().get(1).getTitle());
        verify(todoRepository).findAllByOrderByModifiedAtDesc(pageable);
    }

    @Test
    @DisplayName("Todo 단건 조회 성공")
    void getTodo_Success() {
        // given
        long todoId = 1L;
        User user = new User("test@test.com", "password", UserRole.USER);
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.of(todo));

        // when
        TodoResponse response = todoService.getTodo(todoId);

        // then
        assertNotNull(response);
        assertEquals("Test Title", response.getTitle());
        assertEquals("Test Contents", response.getContents());
        assertEquals("Sunny", response.getWeather());
        verify(todoRepository).findByIdWithUser(todoId);
    }

    @Test
    @DisplayName("Todo 단건 조회 실패 - 존재하지 않는 Todo")
    void getTodo_TodoNotFound() {
        // given
        long todoId = 999L;
        given(todoRepository.findByIdWithUser(anyLong())).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> todoService.getTodo(todoId));
        assertEquals("Todo not found", exception.getMessage());
    }
}