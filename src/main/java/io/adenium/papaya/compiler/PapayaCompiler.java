package io.adenium.papaya.compiler;

import io.adenium.papaya.archive.*;
import io.adenium.papaya.compiler.grammar.Grammar;
import org.json.JSONArray;
import org.json.JSONObject;
import io.adenium.core.ResourceManager;
import io.adenium.crypto.CryptoLib;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.papaya.parser.DynamicParser;
import io.adenium.papaya.parser.Node;
import io.adenium.papaya.parser.Parser;
import io.adenium.papaya.runtime.OpcodeRegister;
import io.adenium.utils.ByteArray;
import io.adenium.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PapayaCompiler extends Compiler {
    private PapayaLexer lexer;
    private Parser      parser;
    private JSONArray   tokens;
    private Map<String, Traverser> traverserMap;
    private OpcodeRegister opcodeRegister;
    private Map<String, Integer> identifiers;
    private ObfuscationStrategy obfuscationStrategy;

    public static void main(String args[]) throws IOException, PapayaException, AdeniumException {
        CryptoLib.getInstance();
        String program = ResourceManager.getString("/papaya/contract.pya");
        String librariesProgram = ResourceManager.getString("/papaya/libraries.pya");
        OpcodeRegister opcodeRegister = new OpcodeRegister();
        OpcodeRegister.register(opcodeRegister);
        Compiler compiler = new PapayaCompiler(opcodeRegister);
        PapayaArchive archive = compiler.createArchive(program, "-identifiers sequential");
        PapayaArchive libraries = compiler.createArchive(librariesProgram, "-identifiers sequential");
        PapayaApplication application = compiler.compile(archive, libraries, "-identifiers sequential");

        System.out.println(application.toString());
    }

    public PapayaCompiler(OpcodeRegister opcodeRegister) throws PapayaException, IOException {
        Grammar grammar = ResourceManager.getGrammar("/papaya/grammar.yml");
        JSONObject papaya = ResourceManager.getJson("/papaya/tokens.json");
        this.opcodeRegister = opcodeRegister;
        identifiers = new HashMap<>();
        traverserMap = new HashMap<>();
        traverserMap.put("int", PapayaCompiler::onEnterInt);
        traverserMap.put("dec", PapayaCompiler::onEnterDec);
        traverserMap.put("string", PapayaCompiler::onEnterString);
        traverserMap.put("ident", PapayaCompiler::onEnterIdent);
        traverserMap.put("variable", PapayaCompiler::onEnterVariableDecl);
        traverserMap.put("ternary_statement", PapayaCompiler::onEnterTernaryStatement);
        traverserMap.put("function_call", PapayaCompiler::onEnterFunctionCall);
        traverserMap.put("member_access", PapayaCompiler::onEnterMemberAccess);
        traverserMap.put("assignment", PapayaCompiler::onAssignment);
        traverserMap.put("return_statement", PapayaCompiler::onReturn);
        traverserMap.put("tuple_expression", PapayaCompiler::onTupleExpression);
        traverserMap.put("tuple", PapayaCompiler::onTuple);
        traverserMap.put("mul_exp", PapayaCompiler::onMul);
        traverserMap.put("add_exp", PapayaCompiler::onAdd);
        traverserMap.put("shift_exp", PapayaCompiler::onShift);
        traverserMap.put("comparison_exp", PapayaCompiler::onComparison);
        traverserMap.put("equality_exp", PapayaCompiler::onEqualityCheck);
        traverserMap.put("cast", PapayaCompiler::onCast);
        traverserMap.put("lambda_expression", PapayaCompiler::onLambdaExpression);
        traverserMap.put(",", PapayaCompiler::onIgnore);
        traverserMap.put(";", PapayaCompiler::onIgnore);

        lexer = new PapayaLexer();
        parser = new DynamicParser(grammar, tokens = papaya.getJSONArray("tokens"));
    }

    private static void onLambdaExpression(Node node, FunctionScope scope, CompilationScope compilationScope) {
        Node statements = node.get("statement*");
        scope.push();
    }

    private static void onCast(Node node, FunctionScope scope, CompilationScope compilationScope) {
        String type[] = getPath(node.at(1));
        System.out.println("err: casts are ignored.");
    }

    private static void onIgnore(Node node, FunctionScope scope, CompilationScope compilationScope) {
    }

    private static void onMul(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node.at(0), scope, compilationScope);
        scope.traverse(node.at(2), scope, compilationScope);
        Node op = node.at(1);
        if (op.equals("*"))
            scope.getWriter().write("mul");
        else if (op.equals("/"))
            scope.getWriter().write("div");
        else if (op.equals("**"))
            scope.getWriter().write("pow");
        else
            scope.getWriter().write("mod");

        if (!node.at(3).isEmpty()) {
            for (Node child : node.at(3)) {
                scope.traverse(child.at(1), scope, compilationScope);

                op = child.at(0);
                if (op.equals("*"))
                    scope.getWriter().write("mul");
                else if (op.equals("/"))
                    scope.getWriter().write("div");
                else if (op.equals("**"))
                    scope.getWriter().write("pow");
                else
                    scope.getWriter().write("mod");
            }
        }
    }

    private static void onAdd(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node.at(0), scope, compilationScope);
        scope.traverse(node.at(2), scope, compilationScope);

        Node op = node.at(1);
        if (op.equals("+"))
            scope.getWriter().write("add");
        else
            scope.getWriter().write("sub");

        if (!node.at(3).isEmpty()) {
            for (Node child : node.at(3)) {
                scope.traverse(child.at(1), scope, compilationScope);

                op = child.at(0);
                if (op.equals("+"))
                    scope.getWriter().write("add");
                else
                    scope.getWriter().write("sub");
            }
        }
    }

    private static void onShift(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node.at(0), scope, compilationScope);
        scope.traverse(node.at(2), scope, compilationScope);

        Node op = node.at(1);
        if (op.equals(">>>"))
            scope.getWriter().write("shf");
        else if (op.equals(">>"))
            scope.getWriter().write("rsh");
        else
            scope.getWriter().write("lsh");

        if (!node.at(3).isEmpty()) {
            for (Node child : node.at(3)) {
                scope.traverse(child.at(1), scope, compilationScope);

                op = child.at(0);
                if (op.equals(">>>"))
                    scope.getWriter().write("shf");
                else if (op.equals(">>"))
                    scope.getWriter().write("rsh");
                else
                    scope.getWriter().write("lsh");
            }
        }
    }

    private static void onComparison(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node.at(0), scope, compilationScope);
        scope.traverse(node.at(2), scope, compilationScope);

        Node op = node.at(1);
        if (op.equals(">="))
            scope.getWriter().write("geq");
        else if (op.equals("<="))
            scope.getWriter().write("leq");
        else if (op.equals(">"))
            scope.getWriter().write("lst");
        else
            scope.getWriter().write("gtn");

        if (!node.at(3).isEmpty()) {
            for (Node child : node.at(3)) {
                scope.traverse(child.at(1), scope, compilationScope);

                op = child.at(0);
                if (op.equals(">="))
                    scope.getWriter().write("geq");
                else if (op.equals("<="))
                    scope.getWriter().write("leq");
                else if (op.equals(">"))
                    scope.getWriter().write("lst");
                else
                    scope.getWriter().write("gtn");
            }
        }
    }

    private static void onEqualityCheck(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node.at(0), scope, compilationScope);
        scope.traverse(node.at(2), scope, compilationScope);
        Node op = node.at(1);
        if (op.equals("=="))
            scope.getWriter().write("eq");
        else
            scope.getWriter().write("neq");

        if (!node.at(3).isEmpty()) {
            for (Node child : node.at(3)) {
                scope.traverse(child.at(1), scope, compilationScope);

                op = child.at(0);
                if (op.equals("=="))
                    scope.getWriter().write("eq");
                else
                    scope.getWriter().write("neq");
            }
        }
    }

    private static void onTuple(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        Node tuple = node.at(1);
        int i = 0;
        do {
            scope.traverse(tuple, scope, compilationScope);
            i ++;

            tuple = tuple.get("tuple_expressions");
        } while (tuple.equals("tuple_expressions"));

        scope.getWriter().writeTuple(i);
    }

    private static void onTupleExpression(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        Node tuple = node.at(0);
        int i = 0;
        do {
            scope.traverse(tuple, scope, compilationScope);
            i ++;

            tuple = node.at(1);
        } while (node.equals("tuple_expressions"));
    }

    private static void onReturn(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        if (node.getChildren().size() > 1) {
            int stackSize = scope.getStack();
            Node expression = node.at(1);
            scope.traverse(expression, scope, compilationScope);
            scope.getWriter().writeReturn(scope.getStack() - stackSize);

            System.out.println(scope.getStack() + " " + stackSize);
        } else {
            scope.getWriter().write("drop");
        }
    }

    private static void onAssignment(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        Node left = node.at(0);
        Node right = node.at(2);
        scope.traverse(right, scope, compilationScope);
        scope.traverse(left, scope, compilationScope);
        scope.getWriter().write("rbp");
    }

    public void traverse(Map<String, Integer> variables, PapayaArchive archive, ArchivedStructureI container, Node node) {
    }

    private static void onEnterMemberAccess(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        int index   = 0;
        LineInfo lineInfo = node.getLineInfo();

        while (!node.isNull()) {
            boolean subscript = node.at(0).at(1).equals("subscript*");

            String name = node.at(0).at(0).getToken().getTokenValue();
            if (index == 0) {
                scope.makeTop(name, lineInfo);
            } else {
                scope.memberAccess(name, lineInfo);
            }

            if (node.at(0).equals("function_call")) {
                scope.getWriter().write("call");
            }

            if (subscript) {
                for (Node subscriptNode : node.at(0).at(1)) {
                    onEnterExpression(subscriptNode.at(1), scope, compilationScope);
                }
            }

            node = node.get("member_access");
            index ++;
        }
    }

    private static void onEnterMemberAccess(Node node, FunctionScope scope, CompilationScope compilationScope, boolean set) throws PapayaException {
        Node member = node.at(0);
        int index   = 0;

        while (!node.isNull()) {
            boolean subscript = member.at(1).equals("subscript*");

            String name = member.at(0).getToken().getTokenValue();
            scope.makeTop(name, node.getLineInfo());
            if (member.equals("function_call")) {
                scope.getWriter().write("call");
            }

            if (subscript) {
                for (Node subscriptNode : member.at(1)) {
                    onEnterExpression(subscriptNode.at(1), scope, compilationScope);
                }
            }

            node = node.get("member_access");
            index ++;
        }
    }

    private static void onEnterIdent(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        String ident = node.getToken().getTokenValue();
        if (scope.contains(ident)) {
            scope.makeTop(ident, node.getLineInfo());
        } else {
            throw new PapayaException("reference to undeclared identifier '" + ident + "' at " + node.getLineInfo());
        }
    }

    private static void onEnterFunctionCall(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        String name = node.at(0).getToken().getTokenValue();
        scope.makeTop(name, node.getLineInfo());
        scope.getWriter().write("call");
    }

    private static void onEnterTernaryStatement(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node.get("expression"), scope, compilationScope);
        FunctionScope clone = scope.push();
        scope.traverse(node.get("function_call"), clone, compilationScope);
        ProgramWriter writer = clone.getWriter();
        scope.getWriter().writeJnt(writer);
    }

    private static void onEnterVariableDecl(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        String name = node.at(1).getToken().getTokenValue();
        String type[] = getPath(node.at(0));
        scope.declare(name, type, node.getLineInfo());

        Node exp = node.get("expression");
        if (!exp.isNull()) {
            scope.traverse(exp, scope, compilationScope);
        } else {
            scope.getWriter().write("push");
        }
    }

    private static void onEnterString(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        String string = node.getToken().getTokenValue();
        scope.getWriter().write("pushdata", string.getBytes(StandardCharsets.UTF_8));
    }

    private static void onEnterDec(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        throw new PapayaException("decimals are not yet supported " + node.getLineInfo());
    }

    private static void onEnterInt(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        BigInteger integer = new BigInteger(node.getToken().getTokenValue());
        if (integer.compareTo(BigInteger.ZERO) < 0) {
            long asLong = Long.parseLong(node.getToken().getTokenValue());
            if (Long.toString(asLong).equals(node.getToken().getTokenValue())) {
                int numBits = Utils.numBitsRequired(asLong);
            } else {
            }
            //Todo: imlpement
        } else if (integer.compareTo(new BigInteger("15")) <= 0) {
            scope.getWriter().write("const" + node.getToken().getTokenValue());
        } else {
            if (integer.bitLength() <= 61) {
                scope.getWriter().writeUInt64(Long.parseLong(node.getToken().getTokenValue()), false);
            } else if (integer.bitLength() <= 251) {
                scope.getWriter().writeUInt256(integer, false);
            }
        }
    }

    private static void onEnterExpression(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        scope.traverse(node, scope, compilationScope);
    }

    @Override
    public PapayaArchive createArchive(String text, String compilerArguments) throws PapayaException, AdeniumException {
        String cArgs[]  = compilerArguments.split(" ");
        obfuscationStrategy = new ObfuscationStrategy.KeepNames();

        AbstractSyntaxTree ast = parser.parse(lexer.ingest(text, tokens));
        Node root = ast.getRoot();
        PapayaArchive papayaArchive = new PapayaArchive();

        for (Node structure : root) {
            archive(structure, papayaArchive);
        }

        return papayaArchive;
    }

    private void archive(Node node, ArchivedStructureI structure) throws PapayaException {
        String name = node.get("ident").getToken().getTokenValue();
        StructureType type = StructureType.None;

        if (node.equals("module_declaration")) {
            ArchivedModule module = new ArchivedModule(name, node.getLineInfo());

            Node members = node.get("class_member*");
            Map<String, Node> assignmentsMap = new HashMap<>();

            Node children = node.at(3);

            for (Node child : children) {
                archive(child, module);
            }

            structure.declare(module.getName(), module);
        } else if (node.equals("structure")) {
            switch (node.get("struct_type").at(0).getToken().getTokenValue()) {
                case "struct":
                    type = StructureType.StructType;
                    break;
                case "class":
                    type = StructureType.ClassType;
                    break;
                case "contract":
                    type = StructureType.ContractType;
                    break;
            }

            ArchivedStruct struct = new ArchivedStruct(name, type, node.getLineInfo());

            Node children = node.get("class_member*");

            for (Node child : children) {
                archive(child, struct);
            }

            structure.declare(name, struct);
        } else if (node.equals("class_member")) {
            archive(node.at(0), structure);
        } else if (node.equals("field")) {
            String fieldName = "";
            String enforcedType[] = null;
            AccessModifier accessMod = AccessModifier.None;
            Node expression = null;
            boolean isStatic = false;

            if (node.at(0).equals("function_pointer")) {
                isStatic        = getStaticModifier(node.at(0));
                accessMod       = getAccessModifier(node.at(0), structure);
                fieldName       = node.at(0).getLast("ident").getToken().getTokenValue();
                String arguments= getArguments(node.at(0).at(3));
                String returnP[]= getPath(node.at(0).at(2));
                enforcedType    = new String[2 + returnP.length];
                for (int i = 0; i < returnP.length; i ++) {
                    enforcedType[i] = returnP[i];
                }
                enforcedType[returnP.length] = arguments;
                enforcedType[1 + returnP.length] = node.at(0).at(5).getToken().getTokenValue();
            } else {
                isStatic        = getStaticModifier(node.at(0));
                accessMod       = getAccessModifier(node.at(0), structure);
                fieldName       = node.getLast("ident").getToken().getTokenValue();
                enforcedType    = getPath(node.get("path"));
                Node assignment = node.get("=");
                if (!assignment.isNull()) {
                    expression = node.get("expression");
                }
            }

            ArchivedMember field = new ArchivedMember(
                    fieldName,
                    enforcedType,
                    accessMod,
                    isStatic,
                    node.getLineInfo(),
                    expression
            );

            structure.declare(fieldName, field);
        } else if (node.equals("method")) {
            String fieldName = "";
            String enforcedType = "";
            AccessModifier accessMod = AccessModifier.PrivateAccess;
            boolean isStatic = false;

            isStatic        = getStaticModifier(node.at(0));
            accessMod       = getAccessModifier(node.at(0), structure);
            fieldName       = node.getLast("ident").getToken().getTokenValue();
            enforcedType    = node.get("ident").getToken().getTokenValue();
            Node arguments  = node.get("method_arguments*");
            Node statements = node.get("statement*");
            List<ArchivedMember> args = new ArrayList<>();

            getFunctionArguments(arguments.get("method_arguments"), args);

            structure.declare(fieldName, new ArchivedMethod(fieldName, enforcedType, args, accessMod, isStatic, node.getLineInfo(), statements));
        }
    }

    private void getFunctionArguments(Node arguments, List<ArchivedMember> members) {
        if (arguments.isNull()) return;

        Node field = arguments.get("method_argument");
        Node next = arguments.get("method_arguments");
        members.add(new ArchivedMember(field.get("ident").getToken().getTokenValue(), getPath(field.get("path")), AccessModifier.None, false, field.getLineInfo(), null));
        if (!next.isNull()) {
            getFunctionArguments(next, members);
        }
    }

    private static String[] getPath(Node path) {
        List<String> simplePath = new ArrayList<>();
        addPath(path, simplePath);

        String result[] = new String[simplePath.size()];
        return simplePath.toArray(result);
    }

    private static void addPath(Node path, List<String> list) {
        Node ident = path.get("ident");
        if (!ident.isNull()) {
            list.add(ident.getToken().getTokenValue());
        }

        Node next = path.get("path");
        if (!next.isNull()) {
            addPath(next, list);
        }
    }

    @Override
    public PapayaApplication compile(PapayaArchive archive, PapayaArchive libraries, String compilerArguments) throws PapayaException, AdeniumException, IOException {
        return archive.compile(compilerArguments, new CompilationScope(archive, libraries, opcodeRegister, traverserMap));
    }

    @Override
    public Expression compile(ArchivedStruct parent, ArchivedMember archivedMember, Node expression) throws PapayaException {
//        // self.setMember(memberId, expression.result)
//        Expression result = compileExpression(expression);
//        result.add(opcodeRegister.forName("store"));
//        return compileExpression(expression);
        return null;
    }

//    private Expression compileExpression(Node assignment) throws PapayaException {
//        Expression expression = new Expression(assignment.getLineInfo());
//        traverse(assignment, expression);
//        return expression;
//    }
//
//    private void traverse(Node node, CompilationScope expression) throws PapayaException {
//        if (expression.getLineInfo().isNull() && !node.getLineInfo().isNull()) {
//            expression.setLineInfo(node.getLineInfo());
//        }
//
//        if (traverserMap.containsKey(node.getTokenRule())) {
//            traverserMap.get(node.getTokenRule()).onEnter(node, expression, opcodeRegister);
//        }
//
//        for (Node child : node) {
//            traverse(child, expression);
//        }
//    }

    private String getArguments(Node paths) {
        return "null";
    }

    private static AccessModifier getAccessModifier(Node member, ArchivedStructureI structure) {
        Node access_mod = member.get("access_mod*");
        if (access_mod.isNull()) {
            return structure.getStructureType() == StructureType.StructType ? AccessModifier.PublicAccess : AccessModifier.PrivateAccess;
        }

        switch (access_mod.at(0).getToken().getTokenValue()) {
            case "public":
                return AccessModifier.PublicAccess;
            case "protected":
                return AccessModifier.ProtectedAccess;
            case "private":
                return AccessModifier.PrivateAccess;
            case "const":
                return AccessModifier.ReadOnly;
        }

        return structure.getStructureType() == StructureType.StructType ? AccessModifier.PublicAccess : AccessModifier.PrivateAccess;
    }

    private static boolean getStaticModifier(Node member) {
        return !member.get("static*").isNull();
    }

    private ByteArray getIdentifier(String identifierName) {
        return ByteArray.wrap(identifierName);
//
//        if (identifiers.containsKey(identifierName)) {
//            return ByteArray.wrap(VarInt.writeCompactUInt32(identifiers.get(identifierName), false));
//        }
//
//        int identifier = identifiers.size();
//        identifiers.put(identifierName, identifier);
//
//        return ByteArray.wrap(VarInt.writeCompactUInt32(identifier, false));
    }
}
