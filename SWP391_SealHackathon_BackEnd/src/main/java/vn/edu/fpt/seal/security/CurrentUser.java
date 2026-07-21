package vn.edu.fpt.seal.security;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CurrentUser {
    UUID id;
    String email;
    List<String> roles;
}
