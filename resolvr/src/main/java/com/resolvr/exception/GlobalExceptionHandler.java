package com.resolvr.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
        InvalidIncidentException.class,
        InvalidStateTransitionException.class,
        TechnicianUnavailableException.class,
        TechnicianWorkloadExceededException.class,
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        ConstraintViolationException.class,
        DuplicateIncidentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDomainValidationExceptions(Exception ex,
                                                   HttpServletRequest request,
                                                   RedirectAttributes redirectAttributes,
                                                   Model model) {
        log.warn("Validation failed on {}", request.getRequestURI());
        model.addAttribute("statusCode", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("errorTitle", "Operation Not Permitted by Business Rule");
        model.addAttribute("errorMessage", "Check your entries and access permissions.");
        return "fragments/error";
    }

    @ExceptionHandler({UnauthorizedOperationException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized(RuntimeException ex,
                                     HttpServletRequest request,
                                     Model model) {
        log.warn("Unauthorized operation attempt on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("statusCode", HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "Check your entries and access permissions.");
        return "fragments/error";
    }

    @ExceptionHandler({
        ServiceNotFoundException.class,
        MissingSLAPolicyException.class,
        UserNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(RuntimeException ex,
                                 HttpServletRequest request,
                                 Model model) {
        log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("statusCode", HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorTitle", "Resource Not Found");
        model.addAttribute("errorMessage", "Check your entries and access permissions.");
        return "fragments/error";
    }

    @ExceptionHandler({DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(Model model) {
        model.addAttribute("statusCode", 409);
        model.addAttribute("errorTitle", "Details have changed");
        model.addAttribute("errorMessage", "Refresh the page and check the account or incident before trying again.");
        return "fragments/error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missingPage(Model model) {
        model.addAttribute("statusCode", 404);
        model.addAttribute("errorTitle", "Page not found");
        return "fragments/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         Model model) {
        log.error("Unexpected system error on {}", request.getRequestURI(), ex);
        model.addAttribute("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("errorTitle", "System Error Encountered");
        model.addAttribute("errorMessage", "Please try again. If the problem continues, contact the service desk.");
        return "fragments/error";
    }
}
