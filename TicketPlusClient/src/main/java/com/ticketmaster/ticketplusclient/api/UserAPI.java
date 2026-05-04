package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.model.UserCreateRequest;
import com.ticketmaster.ticketplusclient.model.UserDTO;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 *
 * @author eriks
 */
public interface UserAPI {

    @GET("/api/users")
    Call<List<UserDTO>> getUsers();

    @POST("/api/users")
    Call<UserDTO> createUser(@Body UserCreateRequest request);

    @DELETE("/api/users/{username}")
    Call<Void> deleteUser(@Path("username") String username);
}