package com.securebank.app.models;

public class Account {
    private int accountId;
    private String accountType;
    private double balance;
    private String accountNumber;
    private String ifscCode;
    private String branchName;
    private String createdAt;
    private String fullName;
    private String email;

    public int getAccountId()       { return accountId; }
    public String getAccountType()  { return accountType; }
    public double getBalance()      { return balance; }
    public String getAccountNumber(){ return accountNumber; }
    public String getIfscCode()     { return ifscCode; }
    public String getBranchName()   { return branchName; }
    public String getCreatedAt()    { return createdAt; }
    public String getFullName()     { return fullName; }
    public String getEmail()        { return email; }
}
