package com.securebank.app.models;

import java.util.List;

public class Customer {
    private int customerId;
    private String fullName;
    private String email;
    private String phone;
    private String createdAt;
    private List<Account> accounts;

    public int getCustomerId()          { return customerId; }
    public String getFullName()         { return fullName; }
    public String getEmail()            { return email; }
    public String getPhone()            { return phone; }
    public String getCreatedAt()        { return createdAt; }
    public List<Account> getAccounts()  { return accounts; }
}
