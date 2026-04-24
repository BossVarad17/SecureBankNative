package com.securebank.app.models;

public class Transaction {
    private int rowNum;
    private int transactionId;
    private String transactionDate;
    private String transactionType;
    private String counterparty;
    private String direction;
    private double amount;
    private String status;
    private int totalRecords;
    private int totalPages;

    public int getRowNum()             { return rowNum; }
    public int getTransactionId()      { return transactionId; }
    public String getTransactionDate() { return transactionDate; }
    public String getTransactionType() { return transactionType; }
    public String getCounterparty()    { return counterparty; }
    public String getDirection()       { return direction; }
    public double getAmount()          { return amount; }
    public String getStatus()          { return status; }
    public int getTotalRecords()       { return totalRecords; }
    public int getTotalPages()         { return totalPages; }
}
