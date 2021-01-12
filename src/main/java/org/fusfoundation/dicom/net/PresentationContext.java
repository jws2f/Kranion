/* 
 * The MIT License
 *
 * Copyright 2016 Focused Ultrasound Foundation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.dicom.net;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

public class PresentationContext {

  private int ID;
  private AbstractSyntax abstractSyntax;
  private int result; // 0=Accept, 1=user-rejection, 2=no-reason, 3=abstract syntax not supported, 4=transfer syntax not supported
  private int selectedTransferSyntaxIndex;

  public PresentationContext(int id, AbstractSyntax as) {
    ID = id;
    abstractSyntax = as;
    result = 2;
    selectedTransferSyntaxIndex = -1;
  }

  public int getID() { return ID; }
  public AbstractSyntax getAbstractSyntax() { return abstractSyntax; }
  public void setResult(int res) { result = res; }
  public int getResult() { return result; }
  public void setSelectedTransferSyntaxIndex(int i) { selectedTransferSyntaxIndex = i; }
  public int getSelectedTransferSyntaxIndex() { return selectedTransferSyntaxIndex; }
}
