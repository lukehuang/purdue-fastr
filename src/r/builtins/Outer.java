package r.builtins;


import com.oracle.truffle.runtime.*;

import r.*;
import r.builtins.Apply.*;
import r.builtins.BuiltIn.NamedArgsBuiltIn.*;
import r.data.*;
import r.nodes.*;
import r.nodes.truffle.*;
import r.nodes.truffle.Constant;
import r.nodes.truffle.FunctionCall;


public class Outer {
    private static final String[] paramNames = new String[]{"X", "Y", "FUN"};

    private static final int IX = 0;
    private static final int IY = 1;
    private static final int IFUN = 2;

    public static final CallFactory FACTORY = new CallFactory() {

        @Override
        public RNode create(ASTNode call, RSymbol[] names, RNode[] exprs) {

            AnalyzedArguments a = BuiltIn.NamedArgsBuiltIn.analyzeArguments(names, exprs, paramNames);
            final boolean[] provided = a.providedParams;
            final int[] paramPositions = a.paramPositions;

            if (!provided[IX]) {
                BuiltIn.missingArg(call, paramNames[IX]);
            }
            if (!provided[IY]) {
                BuiltIn.missingArg(call, paramNames[IY]);
            }
            boolean product = false;

            if (provided[IFUN]) {
                RNode fnode = exprs[paramPositions[IFUN]];
                if (fnode instanceof Constant) {
                    RAny value = ((Constant) fnode).execute(null, null);
                    if (value instanceof RString) {
                        RString str = (RString) value;
                        if (str.size() == 1) {
                            if (str.getString(0).equals("*")) {
                                product = true;
                            }
                        }
                    }

                }
            } else {
                product = true;
            }
            if (product) {
                return new MatrixOperation.OuterProduct(call, exprs[paramPositions[IX]], exprs[paramPositions[IY]]);
            }

            int cnArgs = 2 + names.length - 3; // "-2" because both FUN, X, Y
            RSymbol[] cnNames = new RSymbol[cnArgs];
            RNode[] cnExprs = new RNode[cnArgs];
            cnNames[0] = null;
            ValueProvider xArgProvider = new ValueProvider(call);
            cnExprs[0] = xArgProvider;
            ValueProvider yArgProvider = new ValueProvider(call);
            cnExprs[1] = yArgProvider;
            int j = 0;
            for (int i = 0; i < names.length; i++) {
                if (paramPositions[IX] == i || paramPositions[IY] == i || paramPositions[IFUN] == i) {
                    continue;
                }
                cnNames[2 + j] = names[i];
                cnExprs[2 + j] = exprs[i];
                j++;
            }

            final CallableProvider callableProvider = new CallableProvider(call, exprs[paramPositions[IFUN]]);
            final FunctionCall callNode = FunctionCall.getFunctionCall(call, callableProvider, cnNames, cnExprs);

            return new OuterBuiltIn(call, names, exprs, callNode, callableProvider, xArgProvider, yArgProvider) {
                @Override
                public RAny doBuiltIn(RContext context, Frame frame, RAny[] args) {
                    RAny x = args[paramPositions[IX]];
                    RAny y = args[paramPositions[IY]];
                    RAny f = args[paramPositions[IFUN]];

                    return outer(ast, context, frame, x, y, f);
                }
            };
        }
    };

    public abstract static class OuterBuiltIn extends BuiltIn { // note: this class only exists so that we can call updateParent...
        @Stable FunctionCall callNode;
        @Stable CallableProvider callableProvider;
        @Stable ValueProvider xArgProvider;
        @Stable ValueProvider yArgProvider;


        public OuterBuiltIn(ASTNode ast, RSymbol[] argNames, RNode[] argExprs, FunctionCall callNode, CallableProvider callableProvider, ValueProvider xArgProvider, ValueProvider yArgProvider) {
            super(ast, argNames, argExprs);
            this.callNode = updateParent(callNode);
            this.callableProvider = updateParent(callableProvider);
            this.xArgProvider = updateParent(xArgProvider);
            this.yArgProvider = updateParent(yArgProvider);
        }

        public RAny outer(ASTNode ast, RContext context, Frame frame, RAny xarg, RAny yarg, RAny farg) {
            if (!(xarg instanceof RArray && yarg instanceof RArray)) {
                Utils.nyi("unsupported type");
                return null;
            }
            RArray x = ((RArray) xarg).materialize();
            RArray y = ((RArray) yarg).materialize();

            int xsize = x.size();
            int ysize = y.size();

            RArray expy = expandYVector(y, ysize, xsize);
            RArray expx = null;
            if (xsize > 0) {
                int count =  (int) Math.ceil((double) ysize / (double) xsize);
                expx = expandXVector(x, xsize, count);
            } else {
                expx = x;
            }

            xArgProvider.setValue(expx);
            yArgProvider.setValue(expy);
            callableProvider.matchAndSet(ast, context, frame, farg);
            RArray res = (RArray) callNode.execute(context, frame);

            int[] dimx = x.dimensions();
            int[] dimy = y.dimensions();

            int[] dim;
            if (dimx == null) {
                if (dimy == null) {
                    dim = new int[] {xsize, ysize};
                } else {
                    dim = new int[1 + dimy.length];
                    dim[0] = xsize;
                    System.arraycopy(dimy, 0, dim, 1, dimy.length);
                }
            } else {
                if (dimy == null) {
                    dim = new int[dimx.length + 1];
                    System.arraycopy(dimx, 0, dim, 0, dimx.length);
                    dim[dimx.length] = ysize;
                } else {
                    dim = new int[dimx.length + dimy.length];
                    System.arraycopy(dimx, 0, dim, 0, dimx.length);
                    System.arraycopy(dimy, 0, dim, dimx.length, dimy.length);
                }
            }
            return res.setDimensions(dim);
        }
    }

    public static RArray expandYVector(RArray y, int ysize, int count) {
        int size = ysize;
        int nsize = size * count;

        RArray res = Utils.createArray(y, nsize);
        int offset = 0;
        for (int elem = 0; elem < size; elem++) {
            Object v = y.get(elem);
            for (int i = 0; i < count; i++) {
                res.set(offset + i, v);
            }
            offset += count;
        }
        return res;
    }

    public static RArray expandXVector(RArray x, int xsize, int count) {
        int nsize = xsize * count;

        RArray res = Utils.createArray(x, nsize);
        int offset = 0;
        for (int rep = 0; rep < count; rep++) {
            for (int i = 0; i < xsize; i++) {
                res.set(offset + i, x.get(i));
            }
            offset += xsize;
        }
        return res;
    }
}
