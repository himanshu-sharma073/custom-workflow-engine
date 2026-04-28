package com.example.workflow.auth;

import java.util.List;

public class NoopUserContextProvider implements UserContextProvider {
    @Override
    public String getCurrentUser() { return "anonymous"; }

    @Override
    public List<String> getUserRoles() { return List.of(); }

    @Override
    public List<String> getUserGroups() { return List.of(); }
}
