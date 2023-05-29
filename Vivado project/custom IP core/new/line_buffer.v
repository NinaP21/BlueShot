`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 08.03.2023 23:28:46
// Design Name: 
// Module Name: line_buffer
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


module line_buffer 
#(parameter DATA_WIDTH   = 32,
  parameter IMAGE_WIDTH  = 324)
(
    input                   i_clock,
    input                   i_resetn,
    //Slave interface
    input  [DATA_WIDTH-1:0] i_pixel,
    input                   i_pixel_valid,
    //Master interface
    output [23:0]           o_three_pixels,
    input                   i_read_three_pixels
    );
    
    reg [7:0]  image_line [IMAGE_WIDTH+1:0];
    reg [10:0] write_pointer;
    reg [10:0] read_pointer;
    
    integer i;
    
    initial
    begin
        image_line[0]             <= 'hff;
        image_line[IMAGE_WIDTH+1] <= 'hff;
    end
    
    always @(negedge i_clock)
    begin
        if (i_pixel_valid)
        begin
            for (i = 0; i < DATA_WIDTH/8; i = i + 1)
            begin
                image_line[write_pointer + i]   <= i_pixel[i*8+:8];
            end
        end
    end
    
    always @(posedge i_clock)
    begin
        if( !i_resetn )
        begin
            write_pointer <= 'd1;
        end
        else if (i_pixel_valid)
        begin
            if (write_pointer == (IMAGE_WIDTH - 'd3))
            begin
                write_pointer <= 'd1;
            end
            else
            begin
                write_pointer <= write_pointer + 'd4;
            end
        end
    end
    
    always @(posedge i_clock)
    begin
        if( !i_resetn )
        begin
            read_pointer <= 'd0;
        end
        else if (i_read_three_pixels)
        begin
            
            if (read_pointer == (IMAGE_WIDTH - 'd1))
            begin
                read_pointer <= 'd0;
            end
            else
            begin
                read_pointer <= read_pointer + 'd1;
            end
        end
    end
    
    assign o_three_pixels = {image_line[read_pointer + 'd2], image_line[read_pointer + 'd1], image_line[read_pointer]};  
    
endmodule
