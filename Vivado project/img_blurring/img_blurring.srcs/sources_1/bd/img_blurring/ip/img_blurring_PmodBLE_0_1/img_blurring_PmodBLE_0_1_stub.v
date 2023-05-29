// Copyright 1986-2018 Xilinx, Inc. All Rights Reserved.
// --------------------------------------------------------------------------------
// Tool Version: Vivado v.2018.2 (win64) Build 2258646 Thu Jun 14 20:03:12 MDT 2018
// Date        : Sun May 14 21:41:15 2023
// Host        : LAPTOP-7D6BID1H running 64-bit major release  (build 9200)
// Command     : write_verilog -force -mode synth_stub
//               c:/Users/ninap/Desktop/vivado_projects/img_blurring/img_blurring.srcs/sources_1/bd/img_blurring/ip/img_blurring_PmodBLE_0_1/img_blurring_PmodBLE_0_1_stub.v
// Design      : img_blurring_PmodBLE_0_1
// Purpose     : Stub declaration of top-level module interface
// Device      : xc7s50csga324-1
// --------------------------------------------------------------------------------

// This empty module with port declaration file causes synthesis tools to infer a black box for IP.
// The synthesis directives are for Synopsys Synplify support to prevent IO buffer insertion.
// Please paste the declaration into a Verilog source file or add the file as an additional source.
(* X_CORE_INFO = "PmodBLE,Vivado 2018.2" *)
module img_blurring_PmodBLE_0_1(Pmod_out_pin10_i, Pmod_out_pin10_o, 
  Pmod_out_pin10_t, Pmod_out_pin1_i, Pmod_out_pin1_o, Pmod_out_pin1_t, Pmod_out_pin2_i, 
  Pmod_out_pin2_o, Pmod_out_pin2_t, Pmod_out_pin3_i, Pmod_out_pin3_o, Pmod_out_pin3_t, 
  Pmod_out_pin4_i, Pmod_out_pin4_o, Pmod_out_pin4_t, Pmod_out_pin7_i, Pmod_out_pin7_o, 
  Pmod_out_pin7_t, Pmod_out_pin8_i, Pmod_out_pin8_o, Pmod_out_pin8_t, Pmod_out_pin9_i, 
  Pmod_out_pin9_o, Pmod_out_pin9_t, S_AXI_GPIO_araddr, S_AXI_GPIO_arready, 
  S_AXI_GPIO_arvalid, S_AXI_GPIO_awaddr, S_AXI_GPIO_awready, S_AXI_GPIO_awvalid, 
  S_AXI_GPIO_bready, S_AXI_GPIO_bresp, S_AXI_GPIO_bvalid, S_AXI_GPIO_rdata, 
  S_AXI_GPIO_rready, S_AXI_GPIO_rresp, S_AXI_GPIO_rvalid, S_AXI_GPIO_wdata, 
  S_AXI_GPIO_wready, S_AXI_GPIO_wstrb, S_AXI_GPIO_wvalid, S_AXI_UART_araddr, 
  S_AXI_UART_arready, S_AXI_UART_arvalid, S_AXI_UART_awaddr, S_AXI_UART_awready, 
  S_AXI_UART_awvalid, S_AXI_UART_bready, S_AXI_UART_bresp, S_AXI_UART_bvalid, 
  S_AXI_UART_rdata, S_AXI_UART_rready, S_AXI_UART_rresp, S_AXI_UART_rvalid, 
  S_AXI_UART_wdata, S_AXI_UART_wready, S_AXI_UART_wstrb, S_AXI_UART_wvalid, 
  BLE_uart_interrupt, s_axi_aclk, s_axi_aresetn)
/* synthesis syn_black_box black_box_pad_pin="Pmod_out_pin10_i,Pmod_out_pin10_o,Pmod_out_pin10_t,Pmod_out_pin1_i,Pmod_out_pin1_o,Pmod_out_pin1_t,Pmod_out_pin2_i,Pmod_out_pin2_o,Pmod_out_pin2_t,Pmod_out_pin3_i,Pmod_out_pin3_o,Pmod_out_pin3_t,Pmod_out_pin4_i,Pmod_out_pin4_o,Pmod_out_pin4_t,Pmod_out_pin7_i,Pmod_out_pin7_o,Pmod_out_pin7_t,Pmod_out_pin8_i,Pmod_out_pin8_o,Pmod_out_pin8_t,Pmod_out_pin9_i,Pmod_out_pin9_o,Pmod_out_pin9_t,S_AXI_GPIO_araddr[8:0],S_AXI_GPIO_arready,S_AXI_GPIO_arvalid,S_AXI_GPIO_awaddr[8:0],S_AXI_GPIO_awready,S_AXI_GPIO_awvalid,S_AXI_GPIO_bready,S_AXI_GPIO_bresp[1:0],S_AXI_GPIO_bvalid,S_AXI_GPIO_rdata[31:0],S_AXI_GPIO_rready,S_AXI_GPIO_rresp[1:0],S_AXI_GPIO_rvalid,S_AXI_GPIO_wdata[31:0],S_AXI_GPIO_wready,S_AXI_GPIO_wstrb[3:0],S_AXI_GPIO_wvalid,S_AXI_UART_araddr[12:0],S_AXI_UART_arready,S_AXI_UART_arvalid,S_AXI_UART_awaddr[12:0],S_AXI_UART_awready,S_AXI_UART_awvalid,S_AXI_UART_bready,S_AXI_UART_bresp[1:0],S_AXI_UART_bvalid,S_AXI_UART_rdata[31:0],S_AXI_UART_rready,S_AXI_UART_rresp[1:0],S_AXI_UART_rvalid,S_AXI_UART_wdata[31:0],S_AXI_UART_wready,S_AXI_UART_wstrb[3:0],S_AXI_UART_wvalid,BLE_uart_interrupt,s_axi_aclk,s_axi_aresetn" */;
  input Pmod_out_pin10_i;
  output Pmod_out_pin10_o;
  output Pmod_out_pin10_t;
  input Pmod_out_pin1_i;
  output Pmod_out_pin1_o;
  output Pmod_out_pin1_t;
  input Pmod_out_pin2_i;
  output Pmod_out_pin2_o;
  output Pmod_out_pin2_t;
  input Pmod_out_pin3_i;
  output Pmod_out_pin3_o;
  output Pmod_out_pin3_t;
  input Pmod_out_pin4_i;
  output Pmod_out_pin4_o;
  output Pmod_out_pin4_t;
  input Pmod_out_pin7_i;
  output Pmod_out_pin7_o;
  output Pmod_out_pin7_t;
  input Pmod_out_pin8_i;
  output Pmod_out_pin8_o;
  output Pmod_out_pin8_t;
  input Pmod_out_pin9_i;
  output Pmod_out_pin9_o;
  output Pmod_out_pin9_t;
  input [8:0]S_AXI_GPIO_araddr;
  output S_AXI_GPIO_arready;
  input S_AXI_GPIO_arvalid;
  input [8:0]S_AXI_GPIO_awaddr;
  output S_AXI_GPIO_awready;
  input S_AXI_GPIO_awvalid;
  input S_AXI_GPIO_bready;
  output [1:0]S_AXI_GPIO_bresp;
  output S_AXI_GPIO_bvalid;
  output [31:0]S_AXI_GPIO_rdata;
  input S_AXI_GPIO_rready;
  output [1:0]S_AXI_GPIO_rresp;
  output S_AXI_GPIO_rvalid;
  input [31:0]S_AXI_GPIO_wdata;
  output S_AXI_GPIO_wready;
  input [3:0]S_AXI_GPIO_wstrb;
  input S_AXI_GPIO_wvalid;
  input [12:0]S_AXI_UART_araddr;
  output S_AXI_UART_arready;
  input S_AXI_UART_arvalid;
  input [12:0]S_AXI_UART_awaddr;
  output S_AXI_UART_awready;
  input S_AXI_UART_awvalid;
  input S_AXI_UART_bready;
  output [1:0]S_AXI_UART_bresp;
  output S_AXI_UART_bvalid;
  output [31:0]S_AXI_UART_rdata;
  input S_AXI_UART_rready;
  output [1:0]S_AXI_UART_rresp;
  output S_AXI_UART_rvalid;
  input [31:0]S_AXI_UART_wdata;
  output S_AXI_UART_wready;
  input [3:0]S_AXI_UART_wstrb;
  input S_AXI_UART_wvalid;
  output BLE_uart_interrupt;
  input s_axi_aclk;
  input s_axi_aresetn;
endmodule
