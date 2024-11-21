# Android NFC Control

### Overview
This Android application enables communication with the ST25DV64KC chip via NFC. 
It serves as a demonstration of using Android NFC APIs to interact with STMicroelectronics' X-NUCLEO-NFC07A1 expansion board. 
The app is designed for a single-page interface where users can transmit and receive data to and from the ST25DV64KC chip using the Fast-Mailbox transfer mode.

### Table of Contents

+ [Features](#features)
+ [Architecture](#architecture)
+ [Dependencies](#architecture)
+ [Build Instructions](#build-instructions)
+ [Usage](#usage)
+ [License](#license)

## Features

+ Single Page Interface:
  + Displays NFC connection status.
  + Allows user input to send data to the NFC chip.
  + Displays received data from the NFC chip.
+ Fast-Mailbox Transfer Mode:
  + Utilizes the high-speed mailbox buffer of the ST25DV64KC for data transfer.
+ ST25DV64KC Chip-Specific Communication:
  + Uses the ST25DV64KC's ISO/IEC 15693 NFC Forum Type 5 capabilities.
  + Employs the Fast-Mailbox buffer for efficient data exchange.
 
## Architecture

The application has a straightforward architecture, focusing on NFC communication.

### Components

+ Main Activity:
  + Displays the UI for data input and connection status.
  + Provides a button to send data to the NFC chip.
+ NFC Controller:
  + Manages the NFC communication.
  + Directly interacts with the ST25DV64KC chip, utilizing its Fast-Mailbox mode for sending and receiving data.
+ UI Layer:
  + Provides user feedback on connection status.
  + Displays sent and received data.

### Key Interactions

+ **Data Sending**: User input text is written to the Fast-Mailbox buffer upon NFC connection or button press.
+ **Data Receiving**: Displays data received from the ST25DV64KC in real-time.

## Dependencies

+ Android Studio: Used to develop the application.
+ STMicroelectronics ST25DV64KC: Requires hardware compatibility with the X-NUCLEO-NFC07A1 expansion board.
+ Android NFC API: Handles NFC interactions.

## Build Instructions

1. Clone the Repository:

   ``` bash
     git clone https://github.com/LaRoomy/Android_NFC_Control.git
   ```

2. Open in Android Studio:
   + Launch Android Studio and open the cloned project.
3. Configure the Project:
   + Verify the minSdkVersion and targetSdkVersion in the build.gradle file match your device's capabilities.
4. Build and Deploy:
   + Connect your Android device via USB.
   + Ensure NFC is enabled on your device.
   + Build and run the app on your connected device.

## Usage

1. Prepare Data:
   - Before establishing an NFC connection, enter the desired data into the text box on the app's main page.

2. Establish NFC Connection:
   - Place your smartphone near the X-NUCLEO-NFC07A1 NFC field.
   - Once the connection is established, the entered data will automatically be transmitted to the ST25DV64KC chip.

3. Modify and Resend Data:
   - After the NFC connection is established, you can edit the data in the text box.
   - Press the "Send Now" button to transmit the updated data to the NFC chip.

4. Receive Data:
   - Any data received from the NFC chip will be displayed in the app's main interface.
  
## License

This project is for demonstration purposes and is licensed under the MIT License.



