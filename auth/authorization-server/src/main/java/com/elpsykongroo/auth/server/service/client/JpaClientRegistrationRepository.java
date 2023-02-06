//package com.elpsykongroo.auth.server.service.client;
//
//import com.elpsykongroo.auth.server.entity.client.Client;
//import com.elpsykongroo.auth.server.repository.client.ClientRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.oauth2.client.registration.ClientRegistration;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.Optional;
//
//@Service
//public class JpaClientRegistrationRepository implements ClientRegistrationRepository {
//    @Autowired
//    private ClientRepository clientRepository;
//
//    @Autowired
//    private JpaRegisteredClientRepository registeredClientRepository;
//
//    @Override
//    public ClientRegistration findByRegistrationId(String registrationId) {
//        Optional<Client> client = clientRepository.findByClientId(registrationId);
//        if (!client.isPresent()) {
//            RegisteredClient registerClient = registeredClientRepository.toObject(client.get());
//            ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
//                    .clientId(registrationId)
//                    .clientName(registerClient.getClientName())
//                    .clientSecret(registerClient.getClientSecret())
//                    .clientAuthenticationMethod()
//                    .
//
//            return null;
//        } else {
//            return null;
//        }
//    }
//}
