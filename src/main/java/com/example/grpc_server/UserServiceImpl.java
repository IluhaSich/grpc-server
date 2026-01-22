package com.example.grpc_server;

import com.example.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@GrpcService // Аннотация, которая регистрирует этот класс как gRPC сервис
public class UserServiceImpl extends com.example.grpc.UserServiceGrpc.UserServiceImplBase {

    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        User user = User.newBuilder()
                .setUserId(UUID.randomUUID().toString())
                .setName(request.getName())
                .build();
        users.put(user.getUserId(),user);

        System.out.println("New User");

        CreateUserResponse response = CreateUserResponse.newBuilder()
                .setUser(user).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
        List<User> allUsers = new ArrayList<>();
        for (String key : users.keySet()) {
            allUsers.add(users.get(key));
        }

        if (allUsers.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Users not found")
                            .asRuntimeException()
            );
            return;
        }

        responseObserver.onNext(
                GetUsersResponse.newBuilder()
                        .addAllUser(allUsers)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getUserDelicacy(GetUserDelicacyRequest request,
                                StreamObserver<GetUserDelicacyResponse> responseObserver) {

        User user = users.get(request.getUserId());

        if (user == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asRuntimeException()
            );
            return;
        }

        responseObserver.onNext(
                GetUserDelicacyResponse.newBuilder()
                        .addAllDelicacy(user.getDelicaciesList())
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void addDelicacyToUser(AddDelicacyToUserRequest request,
                                  StreamObserver<AddDelicacyToUserResponse> responseObserver) {

        User user = users.get(request.getUserId());

        if (user == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asRuntimeException()
            );
            return;
        }

        // Проверка на дупликат
        boolean exists = user.getDelicaciesList().stream()
                .anyMatch(d -> d.getDelicacyId().equals(request.getDelicacyId()));

        if (exists) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("Delicacy already added")
                            .asRuntimeException()
            );
            return;
        }

        Delicacy newDelicacy = Delicacy.newBuilder()
                .setDelicacyId(request.getDelicacyId())
                .build();

        User updatedUser = User.newBuilder(user)
                .addDelicacies(newDelicacy)
                .build();

        users.put(updatedUser.getUserId(), updatedUser);

        responseObserver.onNext(
                AddDelicacyToUserResponse.newBuilder()
                        .setUser(updatedUser)
                        .build()
        );
        responseObserver.onCompleted();
    }


    @Override
    public void removeDelicacyFromUser(RemoveDelicacyFromUserRequest request,
                                       StreamObserver<RemoveDelicacyFromUserResponse> responseObserver) {

        User user = users.get(request.getUserId());

        if (user == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asRuntimeException()
            );
            return;
        }

        List<Delicacy> updatedDelicacies = new ArrayList<>(user.getDelicaciesList());
        boolean removed = updatedDelicacies.removeIf(
                d -> d.getDelicacyId().equals(request.getDelicacyId())
        );

        if (!removed) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Delicacy not found")
                            .asRuntimeException()
            );
            return;
        }

        User updatedUser = User.newBuilder(user)
                .clearDelicacies()
                .addAllDelicacies(updatedDelicacies)
                .build();

        users.put(updatedUser.getUserId(), updatedUser);

        responseObserver.onNext(
                RemoveDelicacyFromUserResponse.newBuilder()
                        .setUser(updatedUser)
                        .build()
        );
        responseObserver.onCompleted();
    }
}
