package io.adenium.papaya.compiler;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.parser.Node;
import io.adenium.papaya.parser.Parser;

import java.util.*;

public class PapayaParser implements Parser {
    public AbstractSyntaxTree ingest(TokenStream stream, Map<String, List<List<String>>> grammar) throws PapayaException {
        AbstractSyntaxTree ast = new AbstractSyntaxTree();
        return ast;
    }

    private TokenStream getTokensTilEOL(int currentLine, TokenStream stream) {
        TokenStream result = new TokenStream();

        while (stream.hasNext()) {
            if (stream.peek().getLine() != currentLine) {
                break;
            }

            result.add(stream.next());
        }

        return result;
    }

    private TokenStream getTokensFollowing(String opener, TokenStream stream, String error) throws PapayaException {
        TokenStream result  = new TokenStream();
        String closer    = "";

        if (!stream.matches(opener)) {
            throw new PapayaException(error);
        }

        // skip the opener symbol.
        stream.next();

        switch (opener) {
            case "(":
                closer = ")";
                break;
            case "{":
                closer = "}";
                break;
            case "[":
                closer = "]";
                break;
            default:
                throw new PapayaException("invalid usage of token '" + opener + "' is never closed.");
        }

        int numOpened = 1;
        int numClosed = 0;

        while (stream.hasNext()) {
            Token next = stream.next();
            if (next.getTokenType().equals(opener)) {
                numOpened ++;
            } else if (next.getTokenType().equals(closer)) {
                numClosed ++;
            }

            if (numOpened == numClosed) {
                return result;
            } else {
                result.add(next);
            }
        }

        throw new PapayaException("token '" + opener + "' is never closed.");
    }

    @Override
    public AbstractSyntaxTree parse(TokenStream stream) throws PapayaException {
        AbstractSyntaxTree tree = new AbstractSyntaxTree();
        while (stream.hasNext()) {
            parseStructure(stream, tree.root);
        }

        return tree;
    }

    private void parseStructure(TokenStream stream, Node parentNode) throws PapayaException {
        boolean is_module = stream.matches("module", "ident");
        boolean is_struct = stream.matches("struct", "ident");
        boolean is_class = stream.matches("class", "ident");
        boolean is_contract = stream.matches("contract", "ident");

        if (is_module) {
        } else if (is_struct) {
        } else if (is_class) {
        } else if (is_contract) {
        }
    }

    private Node parseStruct(TokenStream stream) throws PapayaException {
        Node node   = new Node("struct", "struct", stream.next());
        Node name   = new Node("name", stream.peek().getTokenValue(), stream.next());

        if (!stream.matches("{")) {
            throw new PapayaException("expected a '{' at " + node.getLineInfo());
        }

        Node members= parseMemebers(true, false, false, getTokensFollowing("{", stream, "expected a '{' at " + node.getLineInfo()));

        node.add(node);
        node.add(name);
        node.add(members);

        return node;
    }

    private Node parseMemebers(boolean fields, boolean functions, boolean modifiers, TokenStream stream) {
        while (stream.hasNext()) {
            if (stream.matches("access_modifier", "ident", "ident"));
        }

        return null;
    }

    private Node parseClassMember(TokenStream stream) {
        Node member = parseField(stream);
        if (member == null) {
            member = parseFunction(stream);
        }

        return member;
    }

    private Node parseFunction(TokenStream stream) {
        return null;
    }

    private Node parseField(TokenStream stream) {
        return null;
    }
}
