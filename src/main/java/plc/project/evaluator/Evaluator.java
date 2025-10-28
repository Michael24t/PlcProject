package plc.project.evaluator;

import plc.project.parser.Ast;

import java.util.List;
import java.util.Optional;

public final class Evaluator implements Ast.Visitor<RuntimeValue, EvaluateException> {

    private Scope scope;

    public Evaluator(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public RuntimeValue visit(Ast.Source ast) throws EvaluateException {
        RuntimeValue value = new RuntimeValue.Primitive(null);
        for (var stmt : ast.statements()) {
            value = visit(stmt);
        }
        //TODO: Handle RETURN being called outside of a function.
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        //full program, evaluates all statements handling possibility of
        //return being called from outside function
        //return value of the last evaluated statement

        RuntimeValue result;
        if (ast.value().isPresent()) {
            result = visit(ast.value().get());
        } else {
            result = new RuntimeValue.Primitive(null);
        }


        try {
            scope.define(ast.name(), result);
        } catch (IllegalStateException e) {
            throw new EvaluateException("Variable previously defined: " + ast.name(), Optional.of(ast));
        }

        return result;
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    //for first test
    //TODO not working
    @Override
    public RuntimeValue visit(Ast.Stmt.If ast) throws EvaluateException {
//if statement must have
        //condition must be boolean
        //new scope evaluate all thenbody/elsebody statements
        //return value of last evaluated statement or else NIL

        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.For ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Return ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Expression ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Assignment ast) throws EvaluateException {

        RuntimeValue result = visit(ast.value());

        if (ast.expression() instanceof Ast.Expr.Variable var) {
            try {
                scope.assign(var.name(), result );
            } catch (IllegalStateException e) {
                throw new EvaluateException("undefined " + var.name(), Optional.of(ast));
            }
        }

        else {
            throw new EvaluateException("wrongtarget", Optional.of(ast)
            );
        }

        return result ;
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Literal ast) throws EvaluateException {
        return new RuntimeValue.Primitive(ast.value());
    }
//todo for first test
    @Override
    public RuntimeValue visit(Ast.Expr.Group ast) throws EvaluateException {
        //returns wrapped expression evaluates as group
return visit(ast.expression()); // test
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Binary ast) throws EvaluateException {

        throw new UnsupportedOperationException("TODO"); //TODO
    }

    //todo for first test
    @Override
    public RuntimeValue visit(Ast.Expr.Variable ast) throws EvaluateException {
        //variable name must be in scope
        //return the value of the defined variable

        return scope.resolve(ast.name(), false).orElseThrow(() -> new EvaluateException("variable underdefined " + ast.name(), Optional.of(ast)));
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Property ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Function ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Method ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.ObjectExpr ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    /**
     * Helper function for extracting RuntimeValues of specific types. If type
     * is a subclass of {@link RuntimeValue} the check applies to the value
     * itself, otherwise the value must be a {@link RuntimeValue.Primitive} and
     * the check applies to the primitive value.
     */
    private static <T> Optional<T> requireType(RuntimeValue value, Class<T> type) {
        //To be discussed in lecture
        Optional<Object> unwrapped = RuntimeValue.class.isAssignableFrom(type)
            ? Optional.of(value)
            : requireType(value, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value);
        return (Optional<T>) unwrapped.filter(type::isInstance); //cast checked by isInstance
    }

}
