import React, { useState } from 'react';
import * as webauthnJson from "@github/webauthn-json";

function WebAuthnComponent() {
  const [username, setUsername] = useState('');
  const [registrationOptions, setRegistrationOptions] = useState(null);
  const [publicKeyCredential, setPublicKeyCredential] = useState(null);

  const handleUsernameChange = (event) => {
    setUsername(event.target.value);
  };

  const createUser = async () => {
    try {
      const response = await fetch(`http://localhost:8080/createUser?username=${encodeURIComponent(username)}`, {
        method: 'POST',
      });
      const data = await response.json();
      console.log('User created:', data);
      alert("User created successfully!");
    } catch (error) {
      console.error('Error creating user:', error);
      alert("Failed to create user.");
    }
  };

  const startRegistration = async () => {
    try {
      const response = await fetch(`http://localhost:8080/startRegistration?username=${encodeURIComponent(username)}`, {
        method: 'POST',
      });
      /*

The options object is created with a publicKey property, which is set to the publicKeyCredentialCreationOptions from the server response. 
This is the correct format expected by the webauthnJson.create() function, so it works as expected.
      */
      const data = await response.json();
      const options = { publicKey: data.publicKeyCredentialCreationOptions };
  
      const credential = await webauthnJson.create(options);
    
    

      //The options object is set directly to the publicKeyCredentialCreationOptions from the server response. 
      //This is not the correct format expected by the webauthnJson.create() function, which expects the options to be
    // nested inside a publicKey property.
      // Because the options object is not in the correct format, the webauthnJson.create() function throws an error.
      //
      //
    // 
      //   const data = await response.json();
    //   const options = data.publicKeyCredentialCreationOptions;
    // console.log(options)
    
    //   const credential = await webauthnJson.create(options);
    //   console.log('PublicKeyCredential created:', credential);
  
    // Save the registrationId and the credential for the finishRegistration call
    setPublicKeyCredential({
        registrationId: data.registrationId,
        credential
      });
  
      alert("Passkey registration initiation successful!");
    } catch (error) {
      console.error('Error during registration start:', error);
      alert("Registration initiation failed.");
    }
  };

  const finishRegistration = async () => {
    try {
      const response = await fetch('http://localhost:8080/registration/finish', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(publicKeyCredential)
      });
      // Handle the response from the finish registration endpoint
    } catch (error) {
      console.error('Error during registration finish:', error);
      alert("Registration finish failed.");
    }
  };

  return (
    <div>
      <input
        type="text"
        value={username}
        onChange={handleUsernameChange}
        placeholder="Enter username"
      />
      <button onClick={createUser}>Create User</button>
      <button onClick={startRegistration}>Start Passkey Registration</button>
      <button onClick={finishRegistration}>Finish Passkey Registration</button>
    </div>
  );
}

export default WebAuthnComponent;
