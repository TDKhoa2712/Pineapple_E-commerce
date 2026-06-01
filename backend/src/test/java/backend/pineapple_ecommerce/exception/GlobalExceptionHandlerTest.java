package backend.pineapple_ecommerce.exception;

import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.GlobalExceptionHandler;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─────────────────────────────────────────────────────────────────
    // 400 Bad Request
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("BusinessException → 400 với message gốc")
        void businessException_shouldReturn400() {
            BusinessException ex = new BusinessException("Lỗi nghiệp vụ");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Lỗi nghiệp vụ");
        }

        @Test
        @DisplayName("ConstraintViolationException → 400")
        void constraintViolation_shouldReturn400() {
            ConstraintViolationException ex =
                    new ConstraintViolationException("constraint error", Set.of());

            ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("MethodArgumentNotValidException → 400 với map validation errors")
        void methodArgumentNotValid_shouldReturn400WithFieldErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError = new FieldError("obj", "email", "Email không hợp lệ");
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 401 Unauthorized
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("401 Unauthorized")
    class Unauthorized {

        @Test
        @DisplayName("UnauthorizedException → 401 với message gốc")
        void unauthorizedException_shouldReturn401() {
            UnauthorizedException ex = new UnauthorizedException("Token hết hạn");

            ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorized(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getMessage()).isEqualTo("Token hết hạn");
        }

        @Test
        @DisplayName("BadCredentialsException → 401 với thông báo sai email/password")
        void badCredentials_shouldReturn401WithGenericMessage() {
            BadCredentialsException ex = new BadCredentialsException("bad credentials");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getMessage())
                    .contains("Email").contains("mật khẩu");
        }

        @Test
        @DisplayName("DisabledException → 401 với thông báo chưa kích hoạt")
        void disabledException_shouldReturn401WithActivationMessage() {
            DisabledException ex = new DisabledException("disabled");

            ResponseEntity<ApiResponse<Void>> response = handler.handleDisabled(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getMessage()).contains("kích hoạt");
        }

        @Test
        @DisplayName("LockedException → 401 với thông báo tài khoản bị khoá")
        void lockedException_shouldReturn401WithLockedMessage() {
            LockedException ex = new LockedException("locked");

            ResponseEntity<ApiResponse<Void>> response = handler.handleLocked(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getMessage()).contains("khoá");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 403 Forbidden
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("403 Forbidden")
    class Forbidden {

        @Test
        @DisplayName("AccessDeniedException → 403")
        void accessDenied_shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("forbidden");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getMessage()).contains("quyền");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 404 Not Found
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("404 Not Found")
    class NotFound {

        @Test
        @DisplayName("ResourceNotFoundException → 404 với message gốc")
        void resourceNotFound_shouldReturn404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Product", 1L);

            ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).contains("Product").contains("1");
        }

        @Test
        @DisplayName("ResourceNotFoundException với field → message chứa field và value")
        void resourceNotFoundWithField_shouldIncludeFieldAndValue() {
            ResourceNotFoundException ex =
                    new ResourceNotFoundException("Category", "slug", "trai-cay");

            ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage())
                    .contains("slug").contains("trai-cay");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 500 Internal Server Error
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("500 Internal Server Error")
    class InternalError {

        @Test
        @DisplayName("Exception chung → 500 với thông báo chung")
        void genericException_shouldReturn500() {
            Exception ex = new RuntimeException("Lỗi không mong muốn");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getMessage()).containsIgnoringCase("internal server error");
        }
    }
}