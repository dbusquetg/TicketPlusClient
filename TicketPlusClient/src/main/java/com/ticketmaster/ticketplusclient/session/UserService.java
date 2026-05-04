package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.api.UserAPI;
import com.ticketmaster.ticketplusclient.model.UserCreateRequest;
import com.ticketmaster.ticketplusclient.model.UserDTO;
import java.util.List;
import javax.swing.SwingUtilities;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author eriks
 */
public class UserService {

    private final UserAPI userApi;

    public UserService() {
        this.userApi = ClientAPI.createService(UserAPI.class);
    }

    public void getUsers(ServiceCallback<List<UserDTO>> callback) {
        userApi.getUsers().enqueue(new Callback<List<UserDTO>>() {
            @Override
            public void onResponse(Call<List<UserDTO>> call, Response<List<UserDTO>> response) {
                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error al cargar usuarios: " + response.code());
                    }
                });
            }

            @Override
            public void onFailure(Call<List<UserDTO>> call, Throwable t) {
                SwingUtilities.invokeLater(() -> callback.onError("Sin conexion: " + t.getMessage()));
            }
        });
    }

    public void createUser(String username, String password, String role, ServiceCallback<UserDTO> callback) {
        UserCreateRequest request = new UserCreateRequest(username, password, role);

        userApi.createUser(request).enqueue(new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error al crear usuario: " + response.code());
                    }
                });
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                SwingUtilities.invokeLater(() -> callback.onError("Sin conexion: " + t.getMessage()));
            }
        });
    }

    public void deleteUser(String username, ServiceCallback<Void> callback) {
        userApi.deleteUser(username).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError("Error al eliminar usuario: " + response.code());
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                SwingUtilities.invokeLater(() -> callback.onError("Sin conexion: " + t.getMessage()));
            }
        });
    }

    public interface ServiceCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}