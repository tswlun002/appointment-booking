package capitec.branch.appointment.exeption;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.function.BiFunction;


@Slf4j
@RestControllerAdvice
public class ApplicationHandler extends ResponseEntityExceptionHandler {
    BiFunction<AppException,HttpStatus, ResponseEntity<Object>> response = ResponseEntity::new;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        var message = String.join( ",", ex.getAllErrors()
                .stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList()
        );
        var exc = AppException.builder()
                .status(status.value())
                .statusCodeMessage(HttpStatus.METHOD_NOT_ALLOWED.name())
                .message(message).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();
        log.error("Method argument not valid exception: {},HttpStatus: {}, traceId: {} ", exc, status, headers.get("Trace-Id"));
        return response.apply(exc, (HttpStatus) status);
    }
    @ExceptionHandler({ConstraintViolationException.class})
    public  ResponseEntity<?> constrainsFailed(ConstraintViolationException exception, WebRequest request){
        var exc = AppException.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .statusCodeMessage(HttpStatus.BAD_REQUEST.name())
                .message(exception.getMessage()).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();
        var status = HttpStatus.BAD_REQUEST;
        log.error("Field not valid exception: {},HttpStatus: {}, traceId: {} ", exc, status, request.getHeader("Trace-Id"));
        return response.apply(exc, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        String message = exception.getMessage();
        var exc = AppException.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .statusCodeMessage(HttpStatus.UNAUTHORIZED.name())
                .message(message)
                .path(((ServletWebRequest) request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();

        log.error("User unauthorized to access resource. exception: {},HttpStatus: {}, traceId: {} ", exc, exc.status(), request.getHeader("Trace-Id"));
        return response.apply(exc, HttpStatus.valueOf(exc.status()));
    }
    @ExceptionHandler({NotFoundException.class})
    public  ResponseEntity<?> NotFound(NotFoundException exception, WebRequest request){
        var exc = AppException.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .statusCodeMessage(HttpStatus.NOT_FOUND.name())
                .message(exception.getMessage()).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();

        log.error("Entity not found exception: {},HttpStatus: {}, traceId: {} ", exc, exc.status(), request.getHeader("Trace-Id"));
        return response.apply(exc, HttpStatus.valueOf(exc.status()));
    }
    @ExceptionHandler({EntityAlreadyExistException.class})
    public  ResponseEntity<?> EntityAlreadyExist(EntityAlreadyExistException exception, WebRequest request){
        var exc = AppException.builder()
                .status(HttpStatus.CONFLICT.value())
                .statusCodeMessage(HttpStatus.CONFLICT.name())
                .message(exception.getMessage()).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();

        log.error("Entity already exist exception: {},HttpStatus: {}, traceId: {} ", exc, exc.status(), request.getHeader("Trace-Id"));
        return response.apply(exc, HttpStatus.valueOf(exc.status()));
    }
    @ExceptionHandler({UnAuthorizedException.class})
    public  ResponseEntity<?> EntityAlreadyExist(UnAuthorizedException exception, WebRequest request){
        var exc = AppException.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .statusCodeMessage(HttpStatus.UNAUTHORIZED.name())
                .message(exception.getMessage()).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();

        log.error("User unauthorized exception: {},HttpStatus: {}, traceId: {} ", exc, exc.status(), request.getHeader("Trace-Id"));
        return response.apply(exc, HttpStatus.valueOf(exc.status()));
    }
    @ExceptionHandler({ResponseStatusException.class})
    public  ResponseEntity<?> responseStatusException(ResponseStatusException exception, WebRequest request){
        var exc = AppException.builder()
                .status(exception.getStatusCode().value())
                .statusCodeMessage(HttpStatus.valueOf(exception.getStatusCode().value()).name())
                .message(exception.getReason()).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();

        log.error("ResponseStatusException : {},HttpStatus: {}, traceId: {} ", exc, exc.status(), request.getHeader("Trace-Id"));
        return response.apply(exc, HttpStatus.valueOf(exc.status()));
    }
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         @NonNull HttpHeaders headers,
                                                                         @NonNull HttpStatusCode status,
                                                                         @NonNull WebRequest request) {
        var exc = AppException.builder()
                .status(status.value())
                .statusCodeMessage(HttpStatus.METHOD_NOT_ALLOWED.name())
                .message(ex.getMessage()).
                path(((ServletWebRequest)request).getRequest().getRequestURI())
                .timestamp(LocalDateTime.now().toString())
                .build();
        log.error("Method not supported exception: {},HttpStatus: {} , traceId: {} ", exc, status, request.getHeader("Trace-Id"));
        return  response.apply(exc, (HttpStatus) status);
    }

    public   static boolean instanceOfCustomerError(Exception e) {

        return  e instanceof  NotFoundException || e.getCause() instanceof NotFoundException
                || e instanceof  EntityAlreadyExistException || e.getCause() instanceof  EntityAlreadyExistException
                ||  e instanceof ConstraintViolationException || e.getCause() instanceof ConstraintViolationException
                || e instanceof ResponseStatusException || e.getCause() instanceof InternalServerErrorException
                || e  instanceof MethodArgumentNotValidException || e.getCause() instanceof  MethodArgumentNotValidException
                || e instanceof HttpRequestMethodNotSupportedException || e.getCause() instanceof  HttpRequestMethodNotSupportedException
                || e instanceof InternalServerErrorException || e.getCause() instanceof  InternalServerErrorException
                || (e instanceof ClientErrorException && (
                         ((ClientErrorException) e).getResponse().getStatus() == 409
                        || ((ClientErrorException) e).getResponse().getStatus() == 404
                        || ((ClientErrorException) e).getResponse().getStatus() == 401)
                );


    }

}
