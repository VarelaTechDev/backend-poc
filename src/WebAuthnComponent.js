import React, { useState } from 'react';
import * as webauthnJson from "@github/webauthn-json";

function WebAuthnComponent() {
  const [username, setUsername] = useState('exampleUser');
  const [registrationData, setRegistrationData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleUsernameChange = (event) => {
    setUsername(event.target.value);
  };

  const startRegistration = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/register/start?username=${encodeURIComponent(username)}`, { method: 'POST' });
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
      const response = await fetch('http://localhost:8080/registration/finish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(registrationData)
      });
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

  const signIn = async () => {
    setIsLoading(true);
    try {
      const startResponse = await fetch(`http://localhost:8080/assertion/start?username=${encodeURIComponent(username)}`, { method: 'POST' });
      const startData = await startResponse.json();
      
      if (!startResponse.ok) {
        throw new Error(`Start assertion failed: ${startData.error || 'Unknown error'}`);
      }

      const credential = await webauthnJson.get({ publicKey: startData.publicKeyCredentialRequestOptions });
      
      const finishResponse = await fetch('http://localhost:8080/assertion/finish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          assertionId: startData.assertionId,
          credential: credential
        })
      });

      const finishResult = await finishResponse.text();
      
      if (finishResponse.ok) {
        console.log('Sign-in successful:', finishResult);
        alert('Sign-in successful!');
      } else {
        throw new Error(`Finish assertion failed: ${finishResult}`);
      }
    } catch (error) {
      console.error('Error during sign-in:', error);
      setError("Sign-in failed: " + error.message);
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
      <button onClick={startRegistration} disabled={isLoading}>Start Passkey Registration</button>
      <button onClick={finishRegistration} disabled={isLoading || !registrationData}>Finish Passkey Registration</button>
      <button onClick={signIn} disabled={isLoading}>Sign in with Passkey</button>
    </div>
  );
}

export default WebAuthnComponent;
