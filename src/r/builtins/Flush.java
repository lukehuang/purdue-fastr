package r.builtins;

import r.*;
import r.data.*;
import r.data.internal.*;
import r.errors.*;
import r.nodes.ast.*;
import r.nodes.exec.*;
import r.runtime.*;

/**
 * "flush"
 * 
 * <pre>
 * con -- a connection.
 * </pre>
 */
final class Flush extends CallFactory {

    static final CallFactory _ = new Flush("flush", new String[]{"con",}, null);

    private Flush(String name, String[] params, String[] required) {
        super(name, params, required);
    }

    @Override public RNode create(ASTNode call, RSymbol[] names, RNode[] exprs) {
        check(call, names, exprs);
        return new Builtin.Builtin1(call, names, exprs) {
            @Override public RAny doBuiltIn(Frame frame, RAny arg) {
                if (!(arg instanceof RInt)) { throw Utils.nyi("unsupported argument"); }
                RInt iarg = (RInt) arg;
                if (iarg.size() != 1) { throw Utils.nyi("unsupported argument"); }
                int cindex = iarg.getInt(0);
                Connection con = RContext.getConnection(cindex);
                if (con == null) { throw RError.getInvalidConnection(ast); }
                con.flush(ast);
                return RNull.getNull();
            }
        };
    }
}
