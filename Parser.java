import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.

    Token token;          // current token from the input stream
    Lexer lexer;

    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }

    private String match(TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }

    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok
                + "; saw: " + token);
        System.exit(1);
    }

    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok
                + "; saw: " + token);
        System.exit(1);
    }

    public Program program() {
        // Program --> void main ( ) '{' Declarations Statements '}'
        TokenType[] header = {TokenType.Int, TokenType.Main,
                TokenType.LeftParen, TokenType.RightParen};
        for (int i = 0; i < header.length; i++)   // bypass "int main ( )"
            match(header[i]);

        // student exercise
        match(TokenType.LeftBrace);
        Declarations ds = declarations();
        Block b = statements();
        match(TokenType.RightBrace);

        return new Program(ds, b);  // student exercise
    }

    private Declarations declarations() {
        // Declarations --> { Declaration }
        Declarations ds = new Declarations();
        while (isType()) {
            declaration(ds);
        }
        return ds;  // student exercise
    }

    private void declaration(Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        // student exercise
        Type t = type();
        ds.add(new Declaration(new Variable(match(token.type())), t));
        while (token.type().equals(TokenType.Comma)) {
            match(TokenType.Comma);
            ds.add(new Declaration(new Variable(match(token.type())), t));
        }
        match(TokenType.Semicolon);
    }

    private Type type() {
        // Type  -->  int | bool | float | char
        Type t = null;
        // student exercise
        if (token.type().equals(TokenType.Int)) {
            t = Type.INT;
        } else if (token.type().equals(TokenType.Float)) {
            t = Type.FLOAT;
        } else if (token.type().equals(TokenType.Bool)) {
            t = Type.BOOL;
        } else if (token.type().equals(TokenType.Char)) {
            t = Type.CHAR;
        }
        match(token.type());
        return t;
    }

    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement
        Statement s = new Skip();
        // student exercise
        if (token.type().equals(TokenType.Semicolon)) {
            match(TokenType.Semicolon);
        } else if (token.type().equals(TokenType.LeftBrace)) {
            match(TokenType.LeftBrace);
            s = statements();
            match(TokenType.RightBrace);
        } else if (token.type().equals(TokenType.Identifier)) {
            s = assignment();
        } else if (token.type().equals(TokenType.If)) {
            s = ifStatement();
        } else if (token.type().equals(TokenType.While)) {
            s = whileStatement();
        }
        else {
            error("; | Block | Assignment | IfStatement | WhileStatement");
        }

        return s;
    }

    private Block statements() {
        // Block --> '{' Statements '}'
        Block b = new Block();
        // student exercise
        while (isStatement()) {
            b.members.add(statement());
        }

        return b;
    }

    private Assignment assignment() {
        // Assignment --> Identifier = Expression ;
        Variable v = new Variable(match(token.type()));
        match(TokenType.Assign);
        Expression e = expression();

        return new Assignment(v, e);  // student exercise
    }

    private Conditional ifStatement() {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        Conditional c;
        match(TokenType.If);

        Expression e = expression();
        Statement s = statement();
        if (token.type().equals(TokenType.Else)) {
            match(TokenType.Else);
            Statement elseStmt = statement();
            c = new Conditional(e, s, elseStmt);
        } else {
            c = new Conditional(e, s);
        }
        return c;  // student exercise
    }

    private Loop whileStatement() {
        // WhileStatement --> while ( Expression ) Statement
        match(TokenType.While);
        return new Loop(expression(), statement());  // student exercise
    }

    private Expression expression() {
        // Expression --> Conjunction { || Conjunction }
        Expression e = conjunction();
        while (token.type().equals(TokenType.Or)) {
            Operator op = new Operator(match(TokenType.Or));
            Expression conjucntion2 = conjunction();
            e = new Binary(op, e, conjucntion2);
        }
        return e;  // student exercise
    }

    private Expression conjunction() {
        // Conjunction --> Equality { && Equality }
        Expression e = equality();
        while (token.type().equals(TokenType.And)) {
            Operator op = new Operator(match(TokenType.And));
            Expression equality2 = equality();
            e = new Binary(op, e, equality2);
        }
        return e;  // student exercise
    }

    private Expression equality() {
        // Equality --> Relation [ EquOp Relation ]
        Expression e = relation();
        while (isEqualityOp()) {
            Operator op = new Operator(match(token.type()));
            Expression relation2 = relation();
            e = new Binary(op, e, relation2);
        }
        return e;  // student exercise
    }

    private Expression relation() {
        // Relation --> Addition [RelOp Addition]
        Expression e = addition();
        while (isRelationalOp()) {
            Operator op = new Operator(match(token.type()));
            Expression addition2 = addition();
            e = new Binary(op, e, addition2);
        }
        return e;  // student exercise
    }

    private Expression addition() {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression term() {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        } else return primary();
    }

    private Expression primary() {
        // Primary --> Identifier | Literal | ( Expression )
        //             | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            e = new Variable(match(TokenType.Identifier));
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();
            match(TokenType.RightParen);
        } else if (isType()) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal() {
        if (token.type().equals(TokenType.IntLiteral)) {
            return new IntValue(Integer.parseInt(match(token.type())));
        } else if (token.type().equals(TokenType.FloatLiteral)) {
            return new FloatValue(Float.parseFloat(match(token.type())));
        } else if (token.type().equals(TokenType.CharLiteral)) {
            return new CharValue(match(token.type()).charAt(0));
        } else if (isBooleanLiteral()) {
            if (token.type().equals(TokenType.True)) {
                return new BoolValue(true);
            } else {
                return new BoolValue(false);
            }
        }
        else {
            error("IntLiteral | FloatLiteral | CharLiteral | BoolLiteral");
        }
        return null;  // student exercise
    }


    private boolean isAddOp() {
        return token.type().equals(TokenType.Plus) ||
                token.type().equals(TokenType.Minus);
    }

    private boolean isMultiplyOp() {
        return token.type().equals(TokenType.Multiply) ||
                token.type().equals(TokenType.Divide);
    }

    private boolean isUnaryOp() {
        return token.type().equals(TokenType.Not) ||
                token.type().equals(TokenType.Minus);
    }

    private boolean isEqualityOp() {
        return token.type().equals(TokenType.Equals) ||
                token.type().equals(TokenType.NotEqual);
    }

    private boolean isRelationalOp() {
        return token.type().equals(TokenType.Less) ||
                token.type().equals(TokenType.LessEqual) ||
                token.type().equals(TokenType.Greater) ||
                token.type().equals(TokenType.GreaterEqual);
    }

    private boolean isType() {
        return token.type().equals(TokenType.Int)
                || token.type().equals(TokenType.Bool)
                || token.type().equals(TokenType.Float)
                || token.type().equals(TokenType.Char);
    }

    private boolean isLiteral() {
        return token.type().equals(TokenType.IntLiteral) ||
                isBooleanLiteral() ||
                token.type().equals(TokenType.FloatLiteral) ||
                token.type().equals(TokenType.CharLiteral);
    }

    private boolean isBooleanLiteral() {
        return token.type().equals(TokenType.True) ||
                token.type().equals(TokenType.False);
    }

    private boolean isStatement() {
        return token.type().equals(TokenType.Semicolon) ||
                token.type().equals(TokenType.Identifier) ||
                token.type().equals(TokenType.If) ||
                token.type().equals(TokenType.While) ||
                token.type().equals(TokenType.LeftBrace);
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();           // display abstract syntax tree
    } //main

} // Parser
