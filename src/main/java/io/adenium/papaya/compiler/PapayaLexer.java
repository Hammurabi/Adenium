package io.adenium.papaya.compiler;

import org.json.JSONArray;
import org.json.JSONObject;
import io.adenium.exceptions.AdeniumException;

import java.util.*;

public class PapayaLexer {
    private static final char EOF = '\0';
    private static final char END = '\n';
    private static final char WTS = ' ';
    private static final char TAB = '\t';

    private final Set<Character>            symbolSet;
    private final Set<Character>            stackableSymbolSet;


    public PapayaLexer() {
        this.symbolSet = new HashSet<>();
        this.stackableSymbolSet = new HashSet<>();

        symbolSet.add('+');
        symbolSet.add('-');
        symbolSet.add('*');
        symbolSet.add('/');
        symbolSet.add('=');
        symbolSet.add('!');
        symbolSet.add('%');
        symbolSet.add('^');
        symbolSet.add('&');
        symbolSet.add('<');
        symbolSet.add('>');
        symbolSet.add('|');
        symbolSet.add('.');
        symbolSet.add(':');
        symbolSet.add(';');
        symbolSet.add('\\');

        stackableSymbolSet.addAll(symbolSet);

        symbolSet.add('~');
        symbolSet.add(',');
        symbolSet.add('$');
        symbolSet.add('#');
        symbolSet.add('@');
        symbolSet.add('?');
        symbolSet.add('(');
        symbolSet.add(')');
        symbolSet.add('[');
        symbolSet.add(']');
        symbolSet.add('{');
        symbolSet.add('}');
        symbolSet.add('?');
    }

    public TokenStream ingest(String program, JSONArray tokens) throws AdeniumException {
        List<TokenBuilder> builderList = new ArrayList<>();
        TokenStream tokenStream = new TokenStream();

        StringBuilder   builder = new StringBuilder();
        char            lastChar= '\0';

        boolean isString = false;
        int line = 1;
        int offset = 0;
        int whitespace = 0;

        TokenBuilder token = null;

        for (int i = 0; i < program.length(); i ++) {
            char current = program.charAt(i);
            char last = i > 0 ? program.charAt(i - 1) : '\0';
            char next = i < program.length() - 1 ? program.charAt(i + 1) : '\0';

            if (current == END)
            {
                builderList.add(token);
                builderList.add(new TokenBuilder());
                token = null;
                offset = 0;
                whitespace = 0;
                line++;
                continue;
            } else if (current == WTS && !(token != null && (token.toString().startsWith("\"") || token.toString().startsWith("\'"))))
            {
                whitespace++;
                builderList.add(token);
                offset ++;
                token = null;
                continue;
            } else if (current == TAB && !(token != null && (token.toString().startsWith("\"") || token.toString().startsWith("\'"))))
            {
                whitespace += 4;
                offset += 4;
                builderList.add(token);
                token = null;
                continue;
            }

            boolean separator   = symbolSet.contains(current);
            boolean wasnull     = token == null;

            if (token == null) {
                token = new TokenBuilder("", line, offset, whitespace);
            }

            isString = (token.toString().startsWith("\"") || token.toString().startsWith("\'"));

            if (isString || (wasnull && (current == '"' || current == '\'')))
            {
                if (current == '"' || current == '\'' || separator)
                {
                    if (last == '\\') token.append(current);
                    else if (token.toString().startsWith(current + ""))
                    {
                        token.append(current);
                        builderList.add(token);
                        token = null;
                    } else token.append(current);
                } else token.append(current);

                offset++;
                continue;
            } else if (separator)
            {
                builderList.add(token);
                token = new TokenBuilder("" + current, line, offset, whitespace);
                builderList.add(token);
                token = null;
            } else token.append(current);

            offset++;
        }

        Iterator<TokenBuilder> iterator = builderList.iterator();
        while (iterator.hasNext()) {
            TokenBuilder tokenBuilder = nextBuilder(iterator);

            if (tokenBuilder == null) {
                continue;
            }

            if (tokenBuilder.isSymbol(stackableSymbolSet)) {
                TokenBuilder exitToken = null;
                while (iterator.hasNext()) {
                    TokenBuilder toke = nextBuilder(iterator);
                    if (toke != null) {
                        if (!toke.isSymbol(stackableSymbolSet)) {
                            exitToken = toke;
                            break;
                        } else if (toke.getOffset() > (tokenBuilder.getOffset() + 1)) {
                            break;
                        }

                        tokenBuilder.append(toke.toString());
                    }
                }

                tokenStream.add(getToken(tokenBuilder.toString(), tokenBuilder.getLine(), tokenBuilder.getOffset(), tokens));
                if (exitToken != null) {
                    tokenStream.add(getToken(exitToken.toString(), exitToken.getLine(), exitToken.getOffset(), tokens));
                }
            } else {
                tokenStream.add(getToken(tokenBuilder.toString(), tokenBuilder.getLine(), tokenBuilder.getOffset(), tokens));
            }
        }

        return tokenStream;
    }

    private static TokenBuilder nextBuilder(Iterator<TokenBuilder> iterator) {
        while (iterator.hasNext()) {
            TokenBuilder tokenBuilder = iterator.next();
            if (tokenBuilder == null) {
                continue;
            }

            if (tokenBuilder.isEmpty()) {
                continue;
            }

            return tokenBuilder;
        }

        return null;
    }

    private String escape(char character) throws AdeniumException {
        switch (character) {
            case 'n':
                return "\n";
            case 't':
                return "\t";
            case '0':
                return "\0";
            case '\'':
                return "'";
            case '"':
                return "\"";
            default:
                throw new AdeniumException("escaping invalid character '" + character + "'.");
        }
    }

    private boolean isSymbol(char character) {
        return symbolSet.contains(character);
    }

    private boolean isString(String string) {
        return string.startsWith("'") || string.startsWith("\"");
    }

    private static Token getToken(String string, int line, int offset, JSONArray typeMap) throws AdeniumException {
        if ((string.startsWith("'") && string.endsWith("'")) || (string.startsWith("\"") && string.endsWith("\""))) {
            String str = "";
            if (string.length() > 2) {
                str = string.substring(1, string.length() - 1);
            }

            return new Token(str, "string", line, offset);
        }

        for (int i = 0; i < typeMap.length(); i ++) {
            JSONObject onlyObject = typeMap.getJSONObject(i);

            String typeName = onlyObject.keys().next();

            JSONArray options = onlyObject.getJSONArray(typeName);
            for (int x = 0; x < options.length(); x ++) {
                String regex = options.getString(x);

                if (string.matches(regex)) {
                    return new Token(string, typeName, line, offset);
                }
            }
        }

        throw new AdeniumException("could not create token for string '" + string + "'" + " at line: " + line + " offset: " + offset + ".");
    }
}
