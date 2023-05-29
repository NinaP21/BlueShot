`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 09.03.2023 00:00:22
// Design Name: 
// Module Name: img_process_core
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////

// i_data_ready signal is driven by the DMA controller and states whether the DMA 
// is ready to accept o_data
// o_data_ready signal theoritically can be equal to i_data_ready,
// which means that the IP core is ready to accept data from the DMA only when
// the DMA is ready to accept the convoled pixels.
// However, practically this is inaccurate because the convolution module has
// four pipeline stages which means that when the DMA controller sets i_data_ready to zero,
// then the IP module will have four pixels inside the convolution module, that will be lost.
// That is why we use a FIFO buffer to temporarily store the convoled pixels before sending
// them to the DMA contrroller.
// The output fifo is programmed to be considered as full when it contains up to 8 pixels.
// The interrupt signal will be edge triggered and it will become high whenever the IP core 
// gets one line buffer empty. That means that one image line has finished being processed.

module img_process_core
#(parameter DATA_WIDTH   = 32,
  parameter IMAGE_WIDTH  = 324)
  (
    input                   axi_clock,
    input                   axi_resetn,
    //Slave AXI interface
    input                   i_data_valid,
    input  [DATA_WIDTH-1:0] i_data,
    output                  o_data_ready,
    //Master AXI interface
    output                  o_data_valid,
    output [DATA_WIDTH-1:0] o_data,
    input                   i_data_ready,
    //Interrupt
    output                  intr
    );
    
    wire [71:0] i_conv_data;
    wire [7:0]  o_conv_data;
    wire [7:0]  o_fifo_conv_data;
    wire        i_conv_data_valid, o_conv_data_valid;
    wire        width_conv_o_ready, o_fifo_conv_data_valid;
    
   assign o_data_ready = 1'b1;
     
    processing_control #(.DATA_WIDTH(DATA_WIDTH),
                         .IMAGE_WIDTH(IMAGE_WIDTH)) 
    img_ctrl(
        .i_clock(axi_clock),
        .i_resetn(axi_resetn),
        .i_pixel(i_data),
        .i_pixel_valid(i_data_valid),
        .o_pixel(i_conv_data),
        .o_pixel_valid(i_conv_data_valid),
        .intr(intr)
    );
    
    kernel_convolution kernel_conv(
        .i_clock(axi_clock),
        .i_pixel_data(i_conv_data),
        .i_pixel_data_valid(i_conv_data_valid),
        .o_convoled_pixel(o_conv_data),
        .o_convoled_pixel_valid(o_conv_data_valid)
    );
    
    fifo_generator_1 output_buffer (
      .wr_rst_busy(),      // output wire wr_rst_busy
      .rd_rst_busy(),      // output wire rd_rst_busy
      .s_aclk(axi_clock),                // input wire s_aclk
      .s_aresetn(axi_resetn),          // input wire s_aresetn
      .s_axis_tvalid(o_conv_data_valid),  // input wire s_axis_tvalid
      .s_axis_tready(),  // output wire s_axis_tready
      .s_axis_tdata(o_conv_data),    // input wire [7 : 0] s_axis_tdata
      .m_axis_tvalid(o_fifo_conv_data_valid),  // output wire m_axis_tvalid
      .m_axis_tready(width_conv_o_ready),  // input wire m_axis_tready
      .m_axis_tdata(o_fifo_conv_data)    // output wire [7 : 0] m_axis_tdata
    );
    
    axis_dwidth_converter width_conv (
      .aclk(axi_clock),                    // input wire aclk
      .aresetn(axi_resetn),              // input wire aresetn
      .s_axis_tvalid(o_fifo_conv_data_valid),  // input wire s_axis_tvalid
      .s_axis_tready(width_conv_o_ready),  // output wire s_axis_tready
      .s_axis_tdata(o_fifo_conv_data),    // input wire [7 : 0] s_axis_tdata
      .m_axis_tvalid(o_data_valid),  // output wire m_axis_tvalid
      .m_axis_tready(i_data_ready),  // input wire m_axis_tready
      .m_axis_tdata(o_data)    // output wire [31 : 0] m_axis_tdata
    );
       
endmodule