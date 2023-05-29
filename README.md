# BlueShot
Communication between FPGA and Android over BLE protocol

<img src="https://github.com/NinaP21/BlueShot/blob/master/logo.png" align="right" alt="BlueShot logo" width="150" height="150">

<p>This project has been implemented in the context of my diploma thesis, while studying at Computer Engineering and Informatics Department (CEID) of the University of Patras. It is about an Android application that captures a low-resolution picture and sends it to an FPGA-based system, via Bluetooth Low Energy (BLE) protocol. Then, the embedded system processes the image by applying a Gaussian blurring filter and then sends the new picture back to the Android device, so that the user can compare the two images. The main subject of the project concerns the BLE communication between the FPGA and the smartphone.</p>

## Tools and Hardware needed

* The Android application has been developped in Android Studio Chipmunk, 2021.2.1 and the code is written in Java. The app targets Android 13 and can operate in all SDK levels starting from Android SDK 26.
* The development board is the Arty S7-50, that contains the Spartan-7 50 FPGA and 256 MB DDR3 SDPRAM.
* Bluetooth Low Energy in Arty S7-50 is supported by attaching a BLE Pmod to a Pmod connector. 

