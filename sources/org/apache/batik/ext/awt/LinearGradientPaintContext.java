/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.ext.awt;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

/**
 * Provides the actual implementation for the LinearGradientPaint
 * This is where the pixel processing is done.
 * 
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 * @see java.awt.PaintContext
 * @see java.awt.Paint
 * @see java.awt.GradientPaint
 */
final class LinearGradientPaintContext extends MultipleGradientPaintContext {
    
    /**
     * The following invariants are used to process the gradient value from 
     * a device space coordinate, (X, Y):
     * g(X, Y) = dgdX*X + dgdY*Y + gc
     */
    private float dgdX, dgdY, gc;    
           
    /** 
     * Constructor for LinearGradientPaintContext.
     *
     *  @param cm {@link ColorModel} that receives
     *  the <code>Paint</code> data. This is used only as a hint.
     *
     *  @param deviceBounds the device space bounding box of the 
     *  graphics primitive being rendered
     *
     *  @param userBounds the user space bounding box of the 
     *  graphics primitive being rendered
     * 
     *  @param t the {@link AffineTransform} from user
     *  space into device space (gradientTransform should be 
     *  concatenated with this)
     *
     *  @param hints the hints that the context object uses to choose
     *  between rendering alternatives
     *
     *  @param start gradient start point, in user space
     *
     *  @param end gradient end point, in user space
     *
     *  @param fractions the fractions specifying the gradient distribution
     *
     *  @param colors the gradient colors
     *
     *  @param cycleMethod either NO_CYCLE, REFLECT, or REPEAT
     *
     *  @param colorSpace which colorspace to use for interpolation, 
     *  either SRGB or LINEAR_RGB
     *
     */
    public LinearGradientPaintContext(ColorModel cm,
                                      Rectangle deviceBounds,
                                      Rectangle2D userBounds,
                                      AffineTransform t,
                                      RenderingHints hints,
                                      Point2D dStart,
                                      Point2D dEnd,
                                      float[] fractions,
                                      Color[] colors, 
                                      MultipleGradientPaint.CycleMethodEnum 
                                      cycleMethod,
                                      MultipleGradientPaint.ColorSpaceEnum 
                                      colorSpace)
        throws NoninvertibleTransformException
    {	
        super(cm, deviceBounds, userBounds, t, hints, fractions, 
              colors, cycleMethod, colorSpace);
        
      	//call superclass method to calculate the gradients
        calculateGradientFractions();

        // Use single precision floating points
        Point2D.Float start = new Point2D.Float((float)dStart.getX(),
                                                (float)dStart.getY());
        Point2D.Float end = new Point2D.Float((float)dEnd.getX(),
                                              (float)dEnd.getY());
        
        // A given point in the raster should take on the same color as its
        // projection onto the gradient vector.
        // Thus, we want the projection of the current position vector
        // onto the gradient vector, then normalized with respect to the
        // length of the gradient vector, giving a value which can be mapped into
        // the range 0-1.
        // projection = currentVector dot gradientVector / length(gradientVector)
        // normalized = projection / length(gradientVector)

        float dx = end.x - start.x; // change in x from start to end
        float dy = end.y - start.y; // change in y from start to end
        float dSq = dx*dx + dy*dy; // total distance squared
	
        //avoid repeated calculations by doing these divides once.
        float constX = dx/dSq;
        float constY = dy/dSq;
	
        dgdX = a00*constX + a10*constY;//incremental change along gradient for +x
        dgdY = a01*constX + a11*constY;//incremental change along gradient for +y

        //constant, incorporates the translation components from the matrix
        gc = (a02-start.x)*constX + (a12-start.y)*constY;	       	
    }

    protected void fillHardNoCycle(int[] pixels, int off, int adjust, 
                              int x, int y, int w, int h) {

        //constant which can be pulled out of the inner loop
        final float initConst = (dgdX*x) + gc;

        for(int i=0; i<h; i++) { //for every row
            //initialize current value to be start.
            float g = initConst + dgdY*(y+i); 
            final int rowLimit = off+w;  // end of row iteration

            if (dgdX == 0) {
                if (g < 0) g = 0;
                if (g > 1) g = 1;

                // Could be a binary search...
                int gradIdx = 0;
                while (gradIdx < gradientsLength) {
                    if (g < fractions[gradIdx+1])
                        break;
                    gradIdx++;
                }

                float delta = (g-fractions[gradIdx]);
                float idx  = ((delta*GRADIENT_SIZE_INDEX)
                              /normalizedIntervals[gradIdx])+0.5f;
                final int val = gradients[gradIdx][(int)idx];
                while (off < rowLimit) {
                    pixels[off++] = val;
                }
            } else {
                int gradSteps;
                int preGradSteps;
                final int preVal, postVal;
                if (dgdX > 0) {
                    gradSteps    = (int)         ((1-g)/dgdX);
                    preGradSteps = (int)Math.ceil((0-g)/dgdX);
                    preVal  = gradients[0][0];
                    postVal = 
                        gradients[gradients.length-1][GRADIENT_SIZE_INDEX];
                } else { // dgdX < 0
                    gradSteps    = (int)         ((0-g)/dgdX);
                    preGradSteps = (int)Math.ceil((1-g)/dgdX);
                    preVal  = 
                        gradients[gradients.length-1][GRADIENT_SIZE_INDEX];
                    postVal = gradients[0][0];
                }

                if (gradSteps > w) 
                    gradSteps = w;
                final int gradLimit    = off + gradSteps;

                if (preGradSteps > 0) {
                    if (preGradSteps > w)
                        preGradSteps = w;
                    final int preGradLimit = off + preGradSteps;

                    while (off < preGradLimit) {
                        pixels[off++] = preVal;
                    }
                    g += dgdX*preGradSteps;
                }
                        
                if (dgdX > 0) {
                    // Could be a binary search...
                    int gradIdx = 0;
                    while (gradIdx < gradientsLength) {
                        if (g < fractions[gradIdx+1])
                            break;
                        gradIdx++;
                    }
                    
                    while (off < gradLimit) {
                        float delta = (g-fractions[gradIdx]);
                        int [] grad = gradients[gradIdx];

                        int steps = 
                            (int)Math.ceil((fractions[gradIdx+1]-g)/dgdX);
                        int subGradLimit = off + steps;
                        if (subGradLimit > gradLimit)
                            subGradLimit = gradLimit;

                        int idx  = (int)(((delta*GRADIENT_SIZE_INDEX)
                                          /normalizedIntervals[gradIdx])
                                         *(1<<16)) + (1<<15);
                        int step = (int)(((dgdX*GRADIENT_SIZE_INDEX)
                                          /normalizedIntervals[gradIdx])
                                         *(1<<16));
                        while (off < subGradLimit) {
                            pixels[off++] = grad[idx>>16];
                            idx += step;
                        }
                        g+=dgdX*steps;
                        gradIdx++;
                    }
                } else {
                    // Could be a binary search...
                    int gradIdx = gradientsLength-1;
                    while (gradIdx >= 0) {
                        if (g > fractions[gradIdx])
                            break;
                        gradIdx--;
                    }
                    
                    while (off < gradLimit) {
                        float delta = (g-fractions[gradIdx]);
                        int [] grad = gradients[gradIdx];

                        int steps        = (int)Math.ceil(delta/-dgdX);
                        int subGradLimit = off + steps;
                        if (subGradLimit > gradLimit)
                            subGradLimit = gradLimit;

                        int idx  = (int)(((delta*GRADIENT_SIZE_INDEX)
                                          /normalizedIntervals[gradIdx])
                                         *(1<<16)) + (1<<15);
                        int step = (int)(((dgdX*GRADIENT_SIZE_INDEX)
                                          /normalizedIntervals[gradIdx])
                                         *(1<<16));
                        while (off < subGradLimit) {
                            pixels[off++] = grad[idx>>16];
                            idx += step;
                        }
                        g+=dgdX*steps;
                        gradIdx--;
                    }
                }

                while (off < rowLimit) {
                    pixels[off++] = postVal;
                }
            }
            off += adjust; //change in off from row to row
        }
    }

    protected void fillSimpleNoCycle(int[] pixels, int off, int adjust, 
                                int x, int y, int w, int h) {
        //constant which can be pulled out of the inner loop
        final float initConst = (dgdX*x) + gc;
        final float      step = dgdX*fastGradientArraySize;
        final int      fpStep = (int)(step*(1<<16));  // fix point step

        for(int i=0; i<h; i++){ //for every row
            //initialize current value to be start.
            float g = initConst + dgdY*(y+i); 
            g *= fastGradientArraySize;
            g += 0.5; // rounding factor...

            final int rowLimit = off+w;  // end of row iteration

            if (dgdX == 0) {
                final int val = gradient[(int)g];
                while (off < rowLimit) {
                    pixels[off++] = val;
                }
            } else {
                int gradSteps;
                int preGradSteps;
                final int preVal, postVal;
                if (dgdX > 0) {
                    gradSteps = (int)((fastGradientArraySize-g)/step);
                    preGradSteps = (int)Math.ceil(0-g/step);
                    preVal  = gradient[0];
                    postVal = gradient[fastGradientArraySize];

                } else { // dgdX < 0
                    gradSteps    = (int)((0-g)/step);
                    preGradSteps = 
                        (int)Math.ceil((fastGradientArraySize-g)/step);
                    preVal  = gradient[fastGradientArraySize];
                    postVal = gradient[0];
                }

                if (gradSteps > w) 
                    gradSteps = w;
                final int gradLimit    = off + gradSteps;

                if (preGradSteps > 0) {
                    if (preGradSteps > w)
                        preGradSteps = w;
                    final int preGradLimit = off + preGradSteps;

                    while (off < preGradLimit) {
                        pixels[off++] = preVal;
                    }
                    g += step*preGradSteps;
                }
                        
                int fpG = (int)(g*(1<<16));
                while (off < gradLimit) {
                    pixels[off++] = gradient[fpG>>16];
                    fpG += fpStep;
                }
                        
                while (off < rowLimit) {
                    pixels[off++] = postVal;
                }
            }
            off += adjust; //change in off from row to row
        }
    }
    
    protected void fillSimpleRepeat(int[] pixels, int off, int adjust, 
                               int x, int y, int w, int h) {

        final float initConst = (dgdX*x) + gc;

        // Limit step to fractional part of
        // fastGradientArraySize (the non fractional part has
        // no affect anyways, and would mess up lots of stuff
        // below).
        float step = (dgdX - (int)dgdX)*fastGradientArraySize;

                // Make it a Positive step (a small negative step is
                // the same as a positive step slightly less than
                // fastGradientArraySize.
        if (step < 0) 
            step += fastGradientArraySize;

        for(int i=0; i<h; i++) { //for every row
            //initialize current value to be start.
            float g = initConst + dgdY*(y+i); 

            // now Limited between -1 and 1.
            g = g-(int)g;
            // put in the positive side.
            if (g < 0)
                g += 1;
                        
            // scale for gradient array... 
            g *= fastGradientArraySize;
            g += 0.5; // rounding factor
            final int rowLimit = off+w;  // end of row iteration
            while (off < rowLimit) {
                int idx = (int)g;
                if (idx >= fastGradientArraySize) {
                    g   -= fastGradientArraySize;
                    idx -= fastGradientArraySize; 
                }
                pixels[off++] = gradient[idx];
                g += step;
            }

            off += adjust; //change in off from row to row
        }
    }


    protected void fillSimpleReflect(int[] pixels, int off, int adjust, 
                                int x, int y, int w, int h) {
        final float initConst = (dgdX*x) + gc;

        for (int i=0; i<h; i++) { //for every row
            //initialize current value to be start.
            float g = initConst + dgdY*(y+i); 

            // now limited g to -2<->2
            g = g - 2*((int)(g/2.0f));

            float step = dgdX;
            // Pull it into the positive half
            if (g < 0) {
                g = -g; //take absolute value
                step = - step;  // Change direction..
            }

            // Now do the same for dgdX. This is safe because
            // any step that is a multiple of 2.0 has no
            // affect, hence we can remove it which the first
            // part does.  The second part simply adds 2.0
            // (which has no affect due to the cylcle) to move
            // all negative step values into the positive
            // side.
            step = step - 2*((int)step/2.0f);
            if (step < 0) 
                step += 2.0;
            final int reflectMax = 2*fastGradientArraySize;

            // Scale for gradient array.
            g    *= fastGradientArraySize;
            g    += 0.5;
            step *= fastGradientArraySize;
            final int rowLimit = off+w;  // end of row iteration
            while (off < rowLimit) {
                int idx = (int)g;
                if (idx >= reflectMax) {
                    g   -= reflectMax;
                    idx -= reflectMax;
                }

                if (idx <= fastGradientArraySize)
                    pixels[off++] = gradient[idx];
                else
                    pixels[off++] = gradient[reflectMax-idx];
                g+= step;
            }

            off += adjust; //change in off from row to row
        }
    }
        
    /**
     * Return a Raster containing the colors generated for the graphics
     * operation.  This is where the area is filled with colors distributed
     * linearly.
     *
     * @param x,y,w,h The area in device space for which colors are
     * generated.
     *
     */
    protected void fillRaster(int[] pixels, int off, int adjust, 
                              int x, int y, int w, int h) {
	
        //constant which can be pulled out of the inner loop
        final float initConst = (dgdX*x) + gc;

        if (!isSimpleLookup) {
            if (cycleMethod == MultipleGradientPaint.NO_CYCLE) {
                fillHardNoCycle(pixels, off, adjust, x, y, w, h);
            }
            else {
                //initialize current value to be start.
                for(int i=0; i<h; i++){ //for every row
                    float g = initConst + dgdY*(y+i); 
                
                    final int rowLimit = off+w;  // end of row iteration
                    while(off < rowLimit){ //for every pixel in this row.
                        //get the color
                        pixels[off++] = indexIntoGradientsArrays(g); 
                        g += dgdX; //incremental change in g
                    }
                    off += adjust; //change in off from row to row
                }
            }
        } else {
            // Simple implementations: just scale index by array size
            
            if (cycleMethod == MultipleGradientPaint.NO_CYCLE)
                fillSimpleNoCycle(pixels, off, adjust, x, y, w, h);
            else if (cycleMethod == MultipleGradientPaint.REPEAT)
                fillSimpleRepeat(pixels, off, adjust, x, y, w, h);
            else //cycleMethod == MultipleGradientPaint.REFLECT
                fillSimpleReflect(pixels, off, adjust, x, y, w, h);
        }
    }
    
    
}
