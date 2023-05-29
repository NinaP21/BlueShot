`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 08.03.2023 23:52:21
// Design Name: 
// Module Name: kernel_convolution
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


module kernel_convolution(
    input            i_clock,
    input     [71:0] i_pixel_data,
    input            i_pixel_data_valid,
    output reg [7:0] o_convoled_pixel,
    output reg       o_convoled_pixel_valid
    );
    
    reg [7:0] i_pixel_data_red [8:0];
    reg [7:0] i_pixel_data_green [8:0];
    reg [7:0] i_pixel_data_blue [8:0];
    
    reg [7:0]  kernel [8:0];
    reg [15:0] kernel_product_red [8:0];
    reg [15:0] kernel_product_green [8:0];
    reg [15:0] kernel_product_blue [8:0];
    
    reg [15:0] kernel_sum_red_temp;
    reg [15:0] kernel_sum_green_temp;
    reg [15:0] kernel_sum_blue_temp;
    
    reg [15:0] kernel_sum_red;
    reg [15:0] kernel_sum_green;
    reg [15:0] kernel_sum_blue;
    
    reg     color_data_valid, kernel_product_valid, kernel_sum_valid;
    integer i;
    
    initial
    begin
        for ( i=0; i<9; i=i+1 )
            kernel[i] = 'd1; 
    end
    
    always @(posedge i_clock)
    begin
        for ( i=0; i<9; i=i+1 )
        begin
            i_pixel_data_red[i]   <= (i_pixel_data[8*i+:8] & 8'b11100000);
            i_pixel_data_green[i] <= (i_pixel_data[8*i+:8] & 8'b00011100) << 3;
            i_pixel_data_blue[i]  <= (i_pixel_data[8*i+:8] & 8'b00000011) << 6;
        end
        color_data_valid <= i_pixel_data_valid;
    end
    
    always @(posedge i_clock)
    begin
        for ( i=0; i<9; i=i+1 )
        begin
            kernel_product_red[i]   = kernel[i] * i_pixel_data_red[i];
            kernel_product_green[i] = kernel[i] * i_pixel_data_green[i];
            kernel_product_blue[i]  = kernel[i] * i_pixel_data_blue[i];
        end
        kernel_product_valid <= color_data_valid;
    end
    
    always @(*)
    begin
        kernel_sum_red_temp   = 'd0;
        kernel_sum_green_temp = 'd0;
        kernel_sum_blue_temp  = 'd0;
        for ( i=0; i<9; i=i+1 )
        begin
            kernel_sum_red_temp   = kernel_sum_red_temp + kernel_product_red[i];
            kernel_sum_green_temp = kernel_sum_green_temp + kernel_product_green[i];
            kernel_sum_blue_temp  = kernel_sum_blue_temp + kernel_product_blue[i];
        end
    end
    
    always @(posedge i_clock)
    begin
        kernel_sum_red   <= kernel_sum_red_temp;
        kernel_sum_green <= kernel_sum_green_temp;
        kernel_sum_blue  <= kernel_sum_blue_temp;
        kernel_sum_valid <= kernel_product_valid;
    end
    
    always @(posedge i_clock)
    begin
        o_convoled_pixel       <= {(kernel_sum_red/9 & 8'b11100000) | ((kernel_sum_green/9 & 8'b11100000) >> 3) | ((kernel_sum_blue/9 & 8'b11000000) >> 6)};
        o_convoled_pixel_valid <= kernel_sum_valid;
    end
    
endmodule
