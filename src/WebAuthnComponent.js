import React, { useState } from 'react';
import * as webauthnJson from "@github/webauthn-json";

function WebAuthnComponent() {
  const [username, setUsername] = useState('exampleUser');
  const [registrationData, setRegistrationData] = useState(null);
  const [assertionData, setAssertionData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleUsernameChange = (event) => {
    setUsername(event.target.value);
  };

  const createUser = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/auth/createUser?username=${encodeURIComponent(username)}`, {
        method: 'POST'
      });
      if (!response.ok) {
        throw new Error('Error creating user');
      }
      alert('User created successfully');
    } catch (error) {
      console.error('Error during user creation:', error);
      setError("User creation failed.");
    } finally {
      setIsLoading(false);
    }
  };

  const startRegistration = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/auth/registration/start?username=${encodeURIComponent(username)}`, {
        method: 'POST'
      });
      if (!response.ok) {
        throw new Error('Error starting registration');
      }
      const data = await response.json();
      const options = { publicKey: data.publicKeyCredentialCreationOptions };

      console.log("Registration options:", options);
      const credential = await webauthnJson.create(options);

      setRegistrationData({
        registrationId: data.registrationId,
        credential
      });
      console.log('PublicKeyCredential created:', credential);
      alert("Passkey registration initiation successful!");
    } catch (error) {
      console.error('Error during registration start:', error);
      setError("Registration initiation failed.");
    } finally {
      setIsLoading(false);
    }
  };

  const finishRegistration = async () => {
    if (!registrationData) {
      alert("No registration data available");
      return;
    }
    setIsLoading(true);

    try {
      const response = await fetch('http://localhost:8080/auth/registration/finish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          registrationId: registrationData.registrationId,
          credential: registrationData.credential
        })
      });
      if (!response.ok) {
        throw new Error('Error finishing registration');
      }
      const result = await response.text();
      console.log('Registration finished:', result);
      alert("Registration successful!");
    } catch (error) {
      console.error('Error during registration finish:', error);
      setError("Registration finish failed.");
    } finally {
      setIsLoading(false);
    }
  };

  const startAuthentication = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/auth/assertion/start?username=${encodeURIComponent(username)}`, {
        method: 'POST'
      });
      if (!response.ok) {
        throw new Error('Error starting authentication');
      }
      const data = await response.json();
      const options = { publicKey: data.publicKeyCredentialRequestOptions };

      console.log("Authentication options:", options);
      const credential = await webauthnJson.get(options);

      setAssertionData({
        assertionId: data.assertionId,
        credential
      });
      console.log('PublicKeyCredential assertion:', credential);
      alert("Passkey authentication initiation successful!");
    } catch (error) {
      console.error('Error during authentication start:', error);
      setError("Authentication initiation failed.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      {isLoading && <p>Loading...</p>}
      {error && <p>Error: {error}</p>}
      <input
        type="text"
        value={username}
        onChange={handleUsernameChange}
        placeholder="Enter username"
      />
      <button onClick={createUser} disabled={isLoading}>Create User</button>
      <button onClick={startRegistration} disabled={isLoading}>Start Passkey Registration</button>
      <button onClick={finishRegistration} disabled={isLoading || !registrationData}>Finish Passkey Registration</button>
      <button onClick={startAuthentication} disabled={isLoading}>Start Authentication Process</button>
    </div>
  );
}
export default WebAuthnComponent;
