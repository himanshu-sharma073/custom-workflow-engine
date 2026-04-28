package com.example.workflow.auth;

import java.util.List;

public interface UserContextProvider {
    String getCurrentUser();
    default List<String> getRoles() { return getUserRoles(); }
    List<String> getUserRoles();
    List<String> getUserGroups();
}
