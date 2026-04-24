package com.securebank.app.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("api/login")
    Call<ResponseBody> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("api/signup")
    Call<ResponseBody> signup(
            @Field("full_name") String fullName,
            @Field("email") String email,
            @Field("phone") String phone,
            @Field("password") String password,
            @Field("confirm_password") String confirmPassword
    );

    @POST("api/logout")
    Call<ResponseBody> logout();

    @GET("api/dashboard")
    Call<ResponseBody> getDashboard();

    @GET("api/profile")
    Call<ResponseBody> getProfile();

    @GET("api/transfer")
    Call<ResponseBody> getTransferData();

    @FormUrlEncoded
    @POST("api/transfer")
    Call<ResponseBody> doTransfer(
            @Field("from_account") String fromAccountId,
            @Field("to_account") String toAccountNumber,
            @Field("amount") String amount,
            @Field("beneficiary_name") String beneficiaryName,
            @Field("bank_name") String bankName,
            @Field("ifsc") String ifsc
    );

    @GET("api/analytics")
    Call<ResponseBody> getAnalytics();

    @GET("api/statement/{account_id}")
    Call<ResponseBody> getStatement(
            @Path("account_id") int accountId,
            @Query("page") int page
    );
}
