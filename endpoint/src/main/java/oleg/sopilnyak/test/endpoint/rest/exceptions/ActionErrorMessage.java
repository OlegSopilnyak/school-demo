package oleg.sopilnyak.test.endpoint.rest.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// inner classes
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionErrorMessage {
    int errorCode;
    private String errorMessage;
    private String errorUrl;
}
