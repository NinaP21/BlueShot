/*
 * main.c
 *
 *  Created on: 21 May 2023
 *      Author: ninap
 */


#include "xaxidma.h"
#include "xaxidma_hw.h"
#include "xuartlite.h"
#include "xparameters.h"
#include "xil_types.h"
#include "xil_cache.h"
#include "xil_printf.h"
#include "xil_io.h"
#include "xintc.h"
#include "PmodBLE.h"
#include <stdlib.h>
#include <unistd.h>

#define IMAGE_WIDTH			324
#define IMAGE_HEIGHT		576
#define IMAGE_SIZE 			IMAGE_WIDTH*IMAGE_HEIGHT
#define INTC_DEVICE_ID 		XPAR_INTC_0_DEVICE_ID
// IRQ numbers
#define INTC_IMG_PROCESS_ID XPAR_MICROBLAZE_0_AXI_INTC_IMG_PROCESS_CORE_0_INTR_INTR

#define MTU 157
#define BLE_UART_AXI_CLOCK_FREQ XPAR_CPU_M_AXI_DP_FREQ_HZ

XIntc interruptController;
XAxiDma xaxidma;
static int index_row = 4;
u8 *img_bytes;

u32 checkHalted(u32 baseAddress, u32 offset);
int SetUpInterruptSystem();
static void imgISR();

int main() {

	char flag[27];
	u32 status;
	u8 stringDetected 		  = 0;
	u32 receivedBytes 		  = 0;
	u32 totalReceivedBytes 	  = 0;
	u32 transmittedBytes 	  = 0;
	u32 totalTransmittedBytes = 0;


	img_bytes = (u8*) malloc((IMAGE_SIZE + 2*IMAGE_WIDTH)*sizeof(u8));
	if (img_bytes <= 0 || img_bytes == NULL) {
			print("malloc failed...\n\r");
			return XST_FAILURE;
		}

	u8 init_dummy[300];

	//---------------------- UART LITE configuration -----------------------
	XUartLite xUartLite;
	status = XUartLite_Initialize(&xUartLite, XPAR_AXI_UARTLITE_0_DEVICE_ID);
	if (status != XST_SUCCESS) {
		print("UART_LITE initialisation failed...\n\r");
		return XST_FAILURE;
	}
	XUartLite_ResetFifos(&xUartLite);

	//---------------------- AXI DMA configuration -----------------------
	XAxiDma_Config *xaxidma_config;
	xaxidma_config = XAxiDma_LookupConfig(XPAR_AXI_DMA_0_DEVICE_ID);
	if (!xaxidma_config)
	{
		print("DMA configuration failed\n");
		return XST_FAILURE;
	}
	status = XAxiDma_CfgInitialize(&xaxidma, xaxidma_config);
	if (status != XST_SUCCESS) {
		print("DMA initialisation failed\n");
		return XST_FAILURE;
	}

	//---------------- Interrupt controller configuration ----------------
	status = SetUpInterruptSystem();
	if (status != XST_SUCCESS)
	{
		print("Interrupt controller system setup failed\n");
		return XST_FAILURE;
	}

	//---------------------- PMOD BLE configuration -----------------------
	PmodBLE bleDevice;
	BLE_Begin (
		&bleDevice,
		XPAR_PMODBLE_0_S_AXI_GPIO_BASEADDR,
		XPAR_PMODBLE_0_S_AXI_UART_BASEADDR,
		BLE_UART_AXI_CLOCK_FREQ,
		115200
	);

	//-------------------------------------------------------------------------------------------------------------------------

	// insert dummy pixel row at the beginning
	for (int i=0; i < IMAGE_WIDTH; i++)
	{
		img_bytes[i] = 255;
	}

	while ( !stringDetected )
	{
		receivedBytes = BLE_RecvData(&bleDevice, &init_dummy[totalReceivedBytes], 1);
		if (receivedBytes != 0)
		{
			xil_printf("%c", init_dummy[totalReceivedBytes]);
			totalReceivedBytes += receivedBytes;

			if ( (totalReceivedBytes >= 72) &&  (strcmp(memcpy(flag, &init_dummy[totalReceivedBytes-27], 27), "%CONN_PARAM,0012,0000,0200%") == 0))
			{
				xil_printf("\n\n\rReceiving image...\n\r");
				totalReceivedBytes = IMAGE_WIDTH;

				while (totalReceivedBytes < (IMAGE_SIZE + IMAGE_WIDTH))
				{
					receivedBytes = BLE_RecvData(&bleDevice, &img_bytes[totalReceivedBytes], MTU);
					if (receivedBytes != 0)
					{
						totalReceivedBytes += receivedBytes;
					}
				}
				stringDetected = 1;
			} else if (totalReceivedBytes == 299)
			{
				totalReceivedBytes = 72;
			}
		}
	}


	// insert dummy pixel row at the end
	for (int i=(IMAGE_SIZE + IMAGE_WIDTH); i < (IMAGE_SIZE + 2*IMAGE_WIDTH); i++)
	{
		img_bytes[i] = 255;
	}

	//-------------------------------------------------------------------------------------------------------------------------


	xil_printf("Processing image...\n\r");

	//Process image
	status = XAxiDma_SimpleTransfer(&xaxidma, (u32)img_bytes, IMAGE_SIZE, XAXIDMA_DEVICE_TO_DMA); //reception
	if (status != XST_SUCCESS) {
		xil_printf("DMA transfer failed for XAXIDMA_DEVICE_TO_DMA\n");
		return XST_FAILURE;
	}

	status = XAxiDma_SimpleTransfer(&xaxidma, (u32)img_bytes, 4*IMAGE_WIDTH, XAXIDMA_DMA_TO_DEVICE); //transmission
	if (status != XST_SUCCESS) {
		print("DMA transfer failed for XAXIDMA_DMA_TO_DEVICE\n");
		return XST_FAILURE;
	}

	while (index_row < IMAGE_HEIGHT + 2) {}

	//-------------------------------------------------------------------------------------------------------------------------

	xil_printf("Sending image...\n\r");
	while (totalTransmittedBytes < IMAGE_SIZE)
	{
		transmittedBytes = BLE_SendData(&bleDevice, &img_bytes[totalTransmittedBytes], MTU);
		if (transmittedBytes != 0)
		{
			totalTransmittedBytes += transmittedBytes;
		}
	}

	xil_printf("Operation accomplished successfully\n\r");

	XIntc_Stop(&interruptController);

	return XST_SUCCESS;
}


u32 checkHalted(u32 baseAddress, u32 offset) {
	u32 status;
	status = ( XAxiDma_ReadReg(baseAddress, offset) ) & XAXIDMA_IDLE_MASK;
	return status;
}

// This ISR gets called when the image process IP core has finished processing
// one image line and has to fill the empty line buffer with pixels.
static void imgISR()
{
	XIntc_Disable(&interruptController, INTC_IMG_PROCESS_ID);

	int status;
	status = checkHalted(XPAR_AXI_DMA_0_BASEADDR, XAXIDMA_SR_OFFSET);
	while (status == 0)
	{
		status = checkHalted(XPAR_AXI_DMA_0_BASEADDR, XAXIDMA_SR_OFFSET);
	}

	if (index_row < IMAGE_HEIGHT + 2)
	{
		status = XAxiDma_SimpleTransfer(&xaxidma, (u32)&img_bytes[index_row*IMAGE_WIDTH], IMAGE_WIDTH, XAXIDMA_DMA_TO_DEVICE); //transmission

		if (status != XST_SUCCESS) {
			if (status == XST_INVALID_PARAM) {
					xil_printf("DMA transfer failed with XST_INVALID_PARAM and i = %d\n\r", index_row);
			} else if (status == XST_FAILURE) {
				xil_printf("DMA transfer failed with XST_FAILURE and i = %d\n\r", index_row);
			}
		}
		index_row = index_row + 1;
	}

	if (index_row >= IMAGE_HEIGHT + 2)
	{
		return;
	}

	XIntc_Enable(&interruptController, INTC_IMG_PROCESS_ID);
}


int SetUpInterruptSystem()
{
	int status;

	status = XIntc_Initialize(&interruptController, INTC_DEVICE_ID);
	if (status != XST_SUCCESS)
	{
		xil_printf("Interrupt controller initialisation failed\n");
		return XST_FAILURE;
	}

	status = XIntc_SelfTest(&interruptController);
	if (status != XST_SUCCESS)
	{
		xil_printf("Interrupt controller self-test failed\n");
		return XST_FAILURE;
	}

	status = XIntc_Connect(&interruptController, INTC_IMG_PROCESS_ID, (XInterruptHandler)imgISR, (void *)0);
	if (status != XST_SUCCESS)
	{
		xil_printf("Image process Interrupt handler connection failed\n");
		return XST_FAILURE;
	}

	status = XIntc_Start(&interruptController, XIN_REAL_MODE);
	if (status != XST_SUCCESS)
	{
		xil_printf("Interrupt Controller start failed\n");
		return XST_FAILURE;
	}

	XIntc_Enable(&interruptController, INTC_IMG_PROCESS_ID);

	Xil_ExceptionInit();
	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)XIntc_InterruptHandler, &interruptController);
	Xil_ExceptionEnable();

	return XST_SUCCESS;
}


