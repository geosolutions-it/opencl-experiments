/*
Copyright (c) 2010-2011, Advanced Micro Devices, Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following
disclaimer. 

Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution. 

Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
derived from this software without specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

If you use the software (in whole or in part), you shall adhere to all applicable U.S., European, and other export
laws, including but not limited to the U.S. Export Administration Regulations ("EAR"), (15 C.F.R. Sections 730 through
774), and E.U. Council Regulation (EC) No 1334/2000 of 22 June 2000.  Further, pursuant to Section 740.6 of the EAR,
you hereby certify that, except pursuant to a license granted by the United States Department of Commerce Bureau of 
Industry and Security or as otherwise permitted pursuant to a License Exception under the U.S. Export Administration 
Regulations ("EAR"), you will not (1) export, re-export or release to a national of a country in Country Groups D:1,
E:1 or E:2 any restricted technology, software, or source code you receive hereunder, or (2) export to Country Groups
D:1, E:1 or E:2 the direct product of such technology or software, if such foreign produced direct product is subject
to national security controls as identified on the Commerce Control List (currently found in Supplement 1 to Part 774
of EAR).  For the most current Country Group listings, or for additional information about the EAR or your obligations
under those regulations, please refer to the U.S. Bureau of Industry and Security's website at http://www.bis.doc.gov/. 

*/

package com.amd.aparapi;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Very simple example on the usage of Aparapi for performing basic image subsmpling.
 * 
 * @author User
 *
 */
public class ImageTest{
	
	/**
	 * Simple, stupid kernel that tries to do subsampling on an GB image
	 * @author User
	 *
	 */
	public static final class DownsampleKernel extends Kernel{
		private int destW;
		private int numBands;
		private int sourceW;
		private byte[] output;
		private byte[] input;

		public int getDestW() {
			return destW;
		}

		public void setDestW(int destW) {
			this.destW = destW;
		}

		public int getNumBands() {
			return numBands;
		}

		public void setNumBands(int numBands) {
			this.numBands = numBands;
		}

		public int getSourceW() {
			return sourceW;
		}

		public void setSourceW(int sourceW) {
			this.sourceW = sourceW;
		}

		public byte[] getOutput() {
			return output;
		}

		public void setOutput(byte[] output) {
			this.output = output;
		}

		public byte[] getInput() {
			return input;
		}

		public void setInput(byte[] input) {
			this.input = input;
		}
		@Override
		public void run() {
            
    		
            // find row and column in dest
            int destRow= getGlobalId(1);
            int destCol= getGlobalId(0);
            
            //handling bands separately
            int band= getGlobalId(2);
            
            // map to column and row in source
            int srcRow=destRow*SCALE_FACTOR;
            int srcCol=destCol*SCALE_FACTOR;
            
            final int destBase=destCol*numBands+destRow*destW*numBands;
            final int srcBase=srcCol*numBands+srcRow*sourceW*numBands;
            output[destBase+band]=input[srcBase+band];
			
		}
		
	}
	/**
	 * Simple, stupid kernel that tries to do subsampling on an GB image
	 * @author User
	 *
	 */
	public static final class BandCombineKernel extends Kernel{
		public void setInputBands(int inputBands) {
			this.inputBands = inputBands;
		}

		private int width;
		private int height;
		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
		private float[] params;
		private int inputBands;
		public int getInputBands() {
			return inputBands;
		}

		public float[] getParams() {
			return params;
		}

		public void setParams(float[] params) {
			this.params = params;
		}
		private byte[] output;
		private byte[] input;

		public byte[] getOutput() {
			return output;
		}

		public void setOutput(byte[] output) {
			this.output = output;
		}

		public byte[] getInput() {
			return input;
		}

		public void setInput(byte[] input) {
			this.input = input;
		}
		@Override
		public void run() {
            
    		
            // find row and column in dest
            int row= getGlobalId(1);
            int col= getGlobalId(0);
            
            //handling bands separately
            int destBand= getGlobalId(2);
            
            
            // destination pixels
            float accum=0;
            for (int c=0; c < inputBands; c++) {
                accum += input[col+row*width+c]*params[c];
            }
            output[col+row*width+destBand] = (byte) (accum+0.5f);            
			
		}
		
	}
	
	public final static int ITERATIONS= 1;
	public static final int SCALE_FACTOR=2;
	public final static boolean GPU_TEST=Boolean.getBoolean("GPU");	
	public final static boolean WRITE=Boolean.getBoolean("WRITE");
	public final static boolean JAI=Boolean.getBoolean("JAI");


   public static void main(String[] _args) throws IOException {

		if (_args.length < 1) {
			throw new IllegalArgumentException(
					"The first and only argument should be a valid absolute path to a png file.");
		}

		// read image
		final BufferedImage image = ImageIO.read(new File(_args[0]));
		final RenderingHints hints = new RenderingHints(
				RenderingHints.KEY_INTERPOLATION,
				AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

		final AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5), hints);
		// visualize(image);

		// get input
		final Raster raster = image.getTile(0, 0);
		final int sourceW = image.getWidth();
		final int sourceH = image.getHeight();
		final SampleModel sm = raster.getSampleModel();
		final int numBands = sm.getNumBands();
		final StringBuilder sb = new StringBuilder();
		sb.append("w=").append(sourceW).append(" h=").append(sourceH).append("\n");
		sb.append("NumBands0").append(numBands).append("\n");
		sb.append("DataType=").append(sm.getDataType()).append("\n");
		sb.append("NumDataElements=").append(sm.getNumDataElements()).append("\n");
		sb.append("TransferType=").append(sm.getTransferType()).append("\n");
		int[] ss = sm.getSampleSize();
		sb.append("SampleSize=").append(ss[0]).append(" ").append(ss[1]).append(" ").append(ss[2]).append("\n");
		System.out.println(sb.toString());
		// final int[] input = raster.getPixels(0, 0, raster.getWidth(),
		// raster.getHeight(), (int[]) null);
		final byte[] input = ((DataBufferByte) raster.getDataBuffer()).getData();
		System.out.println(input.length);

		final DownsampleKernel downSampleKernel = new DownsampleKernel();
		final BandCombineKernel bandCombineKernel= new BandCombineKernel();
		
		// explicit management of the kernel
		downSampleKernel.setExplicit(true);		
		bandCombineKernel.setExplicit(true);
		double totalTime = 0;
		for (int iteration = 0; iteration < ITERATIONS; iteration++) {
			final long iterationStartTime = System.nanoTime();
			// prepare processing

			if (GPU_TEST) {

				// prepare output
				final byte[] output = new byte[input.length / SCALE_FACTOR*SCALE_FACTOR];
				final int destW = sourceW / SCALE_FACTOR;
				final int destH = sourceH / SCALE_FACTOR;

				//initialize the kernel
				downSampleKernel.setDestW(destW);
				downSampleKernel.setInput(input);
				downSampleKernel.setNumBands(numBands);
				downSampleKernel.setOutput(output);
				downSampleKernel.setSourceW(sourceW);
				
				
				bandCombineKernel.setHeight(destH);
				bandCombineKernel.setWidth(destW);
				bandCombineKernel.setParams(new float[]{1/3.0f,1/3.0f,1/3.0f});
				bandCombineKernel.setInput(output);
				bandCombineKernel.setOutput(output);
				bandCombineKernel.setInputBands(numBands);

				// do subsampling.
				downSampleKernel.put(input);
				downSampleKernel.execute(Range.create3D(destW,destH,numBands));
				downSampleKernel.get(output);
				
				// do bandcombine
				bandCombineKernel.put(output);
				bandCombineKernel.execute(Range.create3D(destW,destH,numBands));
				bandCombineKernel.get(output);
				
				if(WRITE){
				 final DataBufferByte db= new DataBufferByte(output,output.length);
				 final WritableRaster r=Raster.createInterleavedRaster(db,destW,destH,destW*numBands,numBands,new int[]{0},new Point(0,0));
				 // final WritableRaster r= Raster.createInterleavedRaster(0, destW, destH, 3, new Point(0,0));
				 // r.setPixels(0, 0, destW, destH, output);
				 final BufferedImage destImage= new
				 BufferedImage(new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE),r,false,null);
				 ImageIO.write(destImage, "png", new File("D:/data/final.png"));
				}
			} else {

				if (JAI) {
					// getTiles is used to force loading otherwise the operation
					// would not be performed
					final RenderedOp ri = ScaleDescriptor.create(image,
							Float.valueOf(1.0f / SCALE_FACTOR),
							Float.valueOf(1.0f / SCALE_FACTOR),
							Float.valueOf(0), Float.valueOf(0), null, null);
					if(WRITE){
						ImageIO.write(ri, "png",new File("D:/data/final.png"));
					}
					
					ri.getTiles();
					ri.dispose();

				} else {
					// this operation works in immediate mode, hence getRGB
					// should be useless
					BufferedImage destImage = op.filter(image, null);
					
					if(WRITE){
						ImageIO.write(destImage, "png",new File("D:/data/final.png"));
					}
					
					destImage.flush();
					destImage=null;
					
					
				}

			}
			// capture time
			totalTime += System.nanoTime() - iterationStartTime;

		}
		
		// Dispose Kernel resources.
		downSampleKernel.dispose();

		// Report target execution mode: GPU or JTP (Java Thread Pool).
		System.out.println("Execution time=" + totalTime / ITERATIONS / 1E9
				+ " sec");
	}

	@SuppressWarnings({ "unused", "serial" })
	private final static void visualize(final BufferedImage image) {
		JComponent viewer = new JComponent(){
	          @Override public void paintComponent(Graphics g) {
	
	             g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
	          }
	       };
	
	       // Set the size of JComponent which displays Mandelbrot image
	       viewer.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
	       JFrame frame = new JFrame("Input Image");
	       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	       // Swing housework to create the frame
	       frame.getContentPane().add(viewer);
	       frame.pack();
	       frame.setLocationRelativeTo(null);
	       frame.setVisible(true);
	}

}
