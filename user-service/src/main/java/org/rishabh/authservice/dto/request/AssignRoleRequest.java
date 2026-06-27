package org.rishabh.authservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.rishabh.authservice.entity.User;

@Getter
@Setter
public class AssignRoleRequest {

    @NotNull(message = "Role is required")
    private User.Role role;
}
