/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BaseJsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.StreamUtils;
import com.badlogic.gdx.utils.StringBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class JsonReader
implements BaseJsonReader {
    private static final byte[] _json_actions = JsonReader.init__json_actions_0();
    private static final short[] _json_key_offsets = JsonReader.init__json_key_offsets_0();
    private static final char[] _json_trans_keys = JsonReader.init__json_trans_keys_0();
    private static final byte[] _json_single_lengths = JsonReader.init__json_single_lengths_0();
    private static final byte[] _json_range_lengths = JsonReader.init__json_range_lengths_0();
    private static final short[] _json_index_offsets = JsonReader.init__json_index_offsets_0();
    private static final byte[] _json_indicies = JsonReader.init__json_indicies_0();
    private static final byte[] _json_trans_targs = JsonReader.init__json_trans_targs_0();
    private static final byte[] _json_trans_actions = JsonReader.init__json_trans_actions_0();
    private static final byte[] _json_eof_actions = JsonReader.init__json_eof_actions_0();
    static final int json_start = 1;
    static final int json_first_final = 35;
    static final int json_error = 0;
    static final int json_en_object = 5;
    static final int json_en_array = 23;
    static final int json_en_main = 1;
    private final Array<JsonValue> elements = new Array(8);
    private final Array<JsonValue> lastChild = new Array(8);
    private JsonValue root;
    private JsonValue current;

    public JsonValue parse(String json) {
        char[] data = json.toCharArray();
        return this.parse(data, 0, data.length);
    }

    public JsonValue parse(Reader reader) {
        try {
            int length;
            char[] data = new char[1024];
            int offset = 0;
            while ((length = reader.read(data, offset, data.length - offset)) != -1) {
                if (length == 0) {
                    char[] newData = new char[data.length * 2];
                    System.arraycopy(data, 0, newData, 0, data.length);
                    data = newData;
                    continue;
                }
                offset += length;
            }
            JsonValue jsonValue = this.parse(data, 0, offset);
            return jsonValue;
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        finally {
            StreamUtils.closeQuietly(reader);
        }
    }

    @Override
    public JsonValue parse(InputStream input) {
        try {
            JsonValue jsonValue = this.parse(new InputStreamReader(input, "UTF-8"));
            return jsonValue;
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        finally {
            StreamUtils.closeQuietly(input);
        }
    }

    @Override
    public JsonValue parse(FileHandle file) {
        try {
            return this.parse(file.reader("UTF-8"));
        }
        catch (Exception ex) {
            throw new SerializationException("Error parsing file: " + file, ex);
        }
    }

    /*
     * Unable to fully structure code
     */
    public JsonValue parse(char[] data, int offset, int length) {
        p = offset;
        eof = pe = length;
        top = 0;
        stack = new int[4];
        s = 0;
        names = new Array<String>(8);
        needsUnescape = false;
        stringIsName = false;
        stringIsUnquoted = false;
        parseRuntimeEx = null;
        debug = false;
        if (debug) {
            System.out.println();
        }
        try {
            cs = 1;
            top = 0;
            _trans = 0;
            _goto_targ = 0;
            block52: while (true) {
                switch (_goto_targ) {
                    case 0: {
                        if (p == pe) {
                            _goto_targ = 4;
                            continue block52;
                        }
                        if (cs == 0) {
                            _goto_targ = 5;
                            continue block52;
                        }
                    }
                    case 1: {
                        _keys = JsonReader._json_key_offsets[cs];
                        _trans = JsonReader._json_index_offsets[cs];
                        _klen = JsonReader._json_single_lengths[cs];
                        if (_klen <= 0) ** GOTO lbl47
                        _lower = _keys;
                        _upper = _keys + _klen - 1;
                        while (_upper >= _lower) {
                            _mid = _lower + (_upper - _lower >> 1);
                            if (data[p] < JsonReader._json_trans_keys[_mid]) {
                                _upper = _mid - 1;
                                continue;
                            }
                            if (data[p] > JsonReader._json_trans_keys[_mid]) {
                                _lower = _mid + 1;
                                continue;
                            }
                            _trans += _mid - _keys;
                            ** GOTO lbl61
                        }
                        _keys += _klen;
                        _trans += _klen;
lbl47:
                        // 2 sources

                        if ((_klen = JsonReader._json_range_lengths[cs]) > 0) {
                            _lower = _keys;
                            _upper = _keys + (_klen << 1) - 2;
                            while (_upper >= _lower) {
                                _mid = _lower + (_upper - _lower >> 1 & -2);
                                if (data[p] < JsonReader._json_trans_keys[_mid]) {
                                    _upper = _mid - 2;
                                    continue;
                                }
                                if (data[p] > JsonReader._json_trans_keys[_mid + 1]) {
                                    _lower = _mid + 2;
                                    continue;
                                }
                                _trans += _mid - _keys >> 1;
                                ** GOTO lbl61
                            }
                            _trans += _klen;
                        }
lbl61:
                        // 5 sources

                        _trans = JsonReader._json_indicies[_trans];
                        cs = JsonReader._json_trans_targs[_trans];
                        if (JsonReader._json_trans_actions[_trans] == 0) ** GOTO lbl253
                        _acts = JsonReader._json_trans_actions[_trans];
                        _nacts = JsonReader._json_actions[_acts++];
                        block55: while (_nacts-- > 0) {
                            switch (JsonReader._json_actions[_acts++]) {
                                case 0: {
                                    stringIsName = true;
                                    break;
                                }
                                case 1: {
                                    value = new String(data, s, p - s);
                                    if (needsUnescape) {
                                        value = this.unescape(value);
                                    }
                                    if (!stringIsName) ** GOTO lbl81
                                    stringIsName = false;
                                    if (debug) {
                                        System.out.println("name: " + value);
                                    }
                                    names.add(value);
                                    ** GOTO lbl129
lbl81:
                                    // 1 sources

                                    v0 = name = names.size > 0 ? (String)names.pop() : null;
                                    if (!stringIsUnquoted) ** GOTO lbl126
                                    if (!value.equals("true")) ** GOTO lbl88
                                    if (debug) {
                                        System.out.println("boolean: " + name + "=true");
                                    }
                                    this.bool(name, true);
                                    ** GOTO lbl129
lbl88:
                                    // 1 sources

                                    if (!value.equals("false")) ** GOTO lbl93
                                    if (debug) {
                                        System.out.println("boolean: " + name + "=false");
                                    }
                                    this.bool(name, false);
                                    ** GOTO lbl129
lbl93:
                                    // 1 sources

                                    if (!value.equals("null")) ** GOTO lbl96
                                    this.string(name, null);
                                    ** GOTO lbl129
lbl96:
                                    // 1 sources

                                    couldBeDouble = false;
                                    couldBeLong = true;
                                    block56: for (i = s; i < p; ++i) {
                                        switch (data[i]) {
                                            case '+': 
                                            case '-': 
                                            case '0': 
                                            case '1': 
                                            case '2': 
                                            case '3': 
                                            case '4': 
                                            case '5': 
                                            case '6': 
                                            case '7': 
                                            case '8': 
                                            case '9': {
                                                continue block56;
                                            }
                                            case '.': 
                                            case 'E': 
                                            case 'e': {
                                                couldBeDouble = true;
                                                couldBeLong = false;
                                                continue block56;
                                            }
                                            default: {
                                                couldBeDouble = false;
                                                couldBeLong = false;
                                                break block56;
                                            }
                                        }
                                    }
                                    if (!couldBeDouble) ** GOTO lbl118
                                    try {
                                        if (debug) {
                                            System.out.println("double: " + name + "=" + Double.parseDouble(value));
                                        }
                                        this.number(name, Double.parseDouble(value), value);
                                        ** GOTO lbl129
                                    }
                                    catch (NumberFormatException i) {
                                        ** GOTO lbl126
                                    }
lbl118:
                                    // 1 sources

                                    if (!couldBeLong) ** GOTO lbl126
                                    if (debug) {
                                        System.out.println("double: " + name + "=" + Double.parseDouble(value));
                                    }
                                    try {
                                        this.number(name, Long.parseLong(value), value);
                                        ** GOTO lbl129
                                    }
                                    catch (NumberFormatException i) {
                                        // empty catch block
                                    }
lbl126:
                                    // 4 sources

                                    if (debug) {
                                        System.out.println("string: " + name + "=" + value);
                                    }
                                    this.string(name, value);
lbl129:
                                    // 7 sources

                                    stringIsUnquoted = false;
                                    s = p;
                                    break;
                                }
                                case 2: {
                                    v1 = name = names.size > 0 ? (String)names.pop() : null;
                                    if (debug) {
                                        System.out.println("startObject: " + name);
                                    }
                                    this.startObject(name);
                                    if (top == stack.length) {
                                        newStack = new int[stack.length * 2];
                                        System.arraycopy(stack, 0, newStack, 0, stack.length);
                                        stack = newStack;
                                    }
                                    stack[top++] = cs;
                                    cs = 5;
                                    _goto_targ = 2;
                                    continue block52;
                                }
                                case 3: {
                                    if (debug) {
                                        System.out.println("endObject");
                                    }
                                    this.pop();
                                    cs = stack[--top];
                                    _goto_targ = 2;
                                    continue block52;
                                }
                                case 4: {
                                    v2 = name = names.size > 0 ? (String)names.pop() : null;
                                    if (debug) {
                                        System.out.println("startArray: " + name);
                                    }
                                    this.startArray(name);
                                    if (top == stack.length) {
                                        newStack = new int[stack.length * 2];
                                        System.arraycopy(stack, 0, newStack, 0, stack.length);
                                        stack = newStack;
                                    }
                                    stack[top++] = cs;
                                    cs = 23;
                                    _goto_targ = 2;
                                    continue block52;
                                }
                                case 5: {
                                    if (debug) {
                                        System.out.println("endArray");
                                    }
                                    this.pop();
                                    cs = stack[--top];
                                    _goto_targ = 2;
                                    continue block52;
                                }
                                case 6: {
                                    start = p - 1;
                                    if (data[p++] == '/') {
                                        while (p != eof && data[p] != '\n') {
                                            ++p;
                                        }
                                        --p;
                                    } else {
                                        while (p + 1 < eof && data[p] != '*' || data[p + 1] != '/') {
                                            ++p;
                                        }
                                        ++p;
                                    }
                                    if (!debug) continue block55;
                                    System.out.println("comment " + new String(data, start, p - start));
                                    break;
                                }
                                case 7: {
                                    if (debug) {
                                        System.out.println("unquotedChars");
                                    }
                                    s = p;
                                    needsUnescape = false;
                                    stringIsUnquoted = true;
                                    if (!stringIsName) ** GOTO lbl214
                                    block59: while (true) {
                                        switch (data[p]) {
                                            case '\\': {
                                                needsUnescape = true;
                                                ** GOTO lbl208
                                            }
                                            case '/': {
                                                if (p + 1 == eof) ** GOTO lbl208
                                                c = data[p + 1];
                                                if (c == '/') ** GOTO lbl230
                                                if (c == '*') {
                                                    break block59;
                                                }
                                                ** GOTO lbl208
                                            }
                                            case '\n': 
                                            case '\r': 
                                            case ':': {
                                                break block59;
                                            }
lbl208:
                                            // 4 sources

                                            default: {
                                                if (!debug) continue block59;
                                                System.out.println("unquotedChar (name): '" + data[p] + "'");
                                                if (++p != eof) continue block59;
                                                break block59;
                                            }
                                        }
                                        break;
                                    }
                                    ** GOTO lbl230
lbl214:
                                    // 1 sources

                                    block60: while (true) {
                                        switch (data[p]) {
                                            case '\\': {
                                                needsUnescape = true;
                                                ** GOTO lbl225
                                            }
                                            case '/': {
                                                if (p + 1 != eof && ((c = data[p + 1]) == '/' || c == '*')) {
                                                    break block60;
                                                }
                                                ** GOTO lbl225
                                            }
                                            case '\n': 
                                            case '\r': 
                                            case ',': 
                                            case ']': 
                                            case '}': {
                                                break block60;
                                            }
lbl225:
                                            // 3 sources

                                            default: {
                                                if (!debug) continue block60;
                                                System.out.println("unquotedChar (value): '" + data[p] + "'");
                                                if (++p != eof) continue block60;
                                                break block60;
                                            }
                                        }
                                        break;
                                    }
lbl230:
                                    // 5 sources

                                    --p;
                                    while (Character.isSpace(data[p])) {
                                        --p;
                                    }
                                    break;
                                }
                                case 8: {
                                    if (debug) {
                                        System.out.println("quotedChars");
                                    }
                                    s = ++p;
                                    needsUnescape = false;
                                    block62: while (true) {
                                        switch (data[p]) {
                                            case '\\': {
                                                needsUnescape = true;
                                                ++p;
                                                ** GOTO lbl248
                                            }
                                            case '\"': {
                                                break block62;
                                            }
lbl248:
                                            // 2 sources

                                            default: {
                                                if (++p != eof) continue block62;
                                                break block62;
                                            }
                                        }
                                        break;
                                    }
                                    --p;
                                }
                            }
                        }
                    }
lbl253:
                    // 3 sources

                    case 2: {
                        if (cs == 0) {
                            _goto_targ = 5;
                            continue block52;
                        }
                        if (++p != pe) {
                            _goto_targ = 1;
                            continue block52;
                        }
                    }
                    case 4: {
                        if (p != eof) break block52;
                        __acts = JsonReader._json_eof_actions[cs];
                        __nacts = JsonReader._json_actions[__acts++];
                        while (__nacts-- > 0) {
                            switch (JsonReader._json_actions[__acts++]) {
                                case 1: {
                                    value = new String(data, s, p - s);
                                    if (needsUnescape) {
                                        value = this.unescape(value);
                                    }
                                    if (!stringIsName) ** GOTO lbl276
                                    stringIsName = false;
                                    if (debug) {
                                        System.out.println("name: " + value);
                                    }
                                    names.add(value);
                                    ** GOTO lbl324
lbl276:
                                    // 1 sources

                                    v3 = name = names.size > 0 ? (String)names.pop() : null;
                                    if (!stringIsUnquoted) ** GOTO lbl321
                                    if (!value.equals("true")) ** GOTO lbl283
                                    if (debug) {
                                        System.out.println("boolean: " + name + "=true");
                                    }
                                    this.bool(name, true);
                                    ** GOTO lbl324
lbl283:
                                    // 1 sources

                                    if (!value.equals("false")) ** GOTO lbl288
                                    if (debug) {
                                        System.out.println("boolean: " + name + "=false");
                                    }
                                    this.bool(name, false);
                                    ** GOTO lbl324
lbl288:
                                    // 1 sources

                                    if (!value.equals("null")) ** GOTO lbl291
                                    this.string(name, null);
                                    ** GOTO lbl324
lbl291:
                                    // 1 sources

                                    couldBeDouble = false;
                                    couldBeLong = true;
                                    block64: for (i = s; i < p; ++i) {
                                        switch (data[i]) {
                                            case '+': 
                                            case '-': 
                                            case '0': 
                                            case '1': 
                                            case '2': 
                                            case '3': 
                                            case '4': 
                                            case '5': 
                                            case '6': 
                                            case '7': 
                                            case '8': 
                                            case '9': {
                                                continue block64;
                                            }
                                            case '.': 
                                            case 'E': 
                                            case 'e': {
                                                couldBeDouble = true;
                                                couldBeLong = false;
                                                continue block64;
                                            }
                                            default: {
                                                couldBeDouble = false;
                                                couldBeLong = false;
                                                break block64;
                                            }
                                        }
                                    }
                                    if (!couldBeDouble) ** GOTO lbl313
                                    try {
                                        if (debug) {
                                            System.out.println("double: " + name + "=" + Double.parseDouble(value));
                                        }
                                        this.number(name, Double.parseDouble(value), value);
                                        ** GOTO lbl324
                                    }
                                    catch (NumberFormatException var29_47) {
                                        ** GOTO lbl321
                                    }
lbl313:
                                    // 1 sources

                                    if (!couldBeLong) ** GOTO lbl321
                                    if (debug) {
                                        System.out.println("double: " + name + "=" + Double.parseDouble(value));
                                    }
                                    try {
                                        this.number(name, Long.parseLong(value), value);
                                        ** GOTO lbl324
                                    }
                                    catch (NumberFormatException var29_48) {
                                        // empty catch block
                                    }
lbl321:
                                    // 4 sources

                                    if (debug) {
                                        System.out.println("string: " + name + "=" + value);
                                    }
                                    this.string(name, value);
lbl324:
                                    // 7 sources

                                    stringIsUnquoted = false;
                                    s = p;
                                }
                            }
                        }
                        break block52;
                    }
                }
                break;
            }
        }
        catch (RuntimeException ex) {
            parseRuntimeEx = ex;
        }
        root = this.root;
        this.root = null;
        this.current = null;
        this.lastChild.clear();
        if (p < pe) {
            lineNumber = 1;
            for (i = 0; i < p; ++i) {
                if (data[i] != '\n') continue;
                ++lineNumber;
            }
            start = Math.max(0, p - 32);
            throw new SerializationException("Error parsing JSON on line " + lineNumber + " near: " + new String(data, start, p - start) + "*ERROR*" + new String(data, p, Math.min(64, pe - p)), parseRuntimeEx);
        }
        if (this.elements.size != 0) {
            element = this.elements.peek();
            this.elements.clear();
            if (element != null && element.isObject()) {
                throw new SerializationException("Error parsing JSON, unmatched brace.");
            }
            throw new SerializationException("Error parsing JSON, unmatched bracket.");
        }
        if (parseRuntimeEx != null) {
            throw new SerializationException("Error parsing JSON: " + new String(data), parseRuntimeEx);
        }
        return root;
    }

    private static byte[] init__json_actions_0() {
        return new byte[]{0, 1, 1, 1, 2, 1, 3, 1, 4, 1, 5, 1, 6, 1, 7, 1, 8, 2, 0, 7, 2, 0, 8, 2, 1, 3, 2, 1, 5};
    }

    private static short[] init__json_key_offsets_0() {
        return new short[]{0, 0, 11, 13, 14, 16, 25, 31, 37, 39, 50, 57, 64, 73, 74, 83, 85, 87, 96, 98, 100, 101, 103, 105, 116, 123, 130, 141, 142, 153, 155, 157, 168, 170, 172, 174, 179, 184, 184};
    }

    private static char[] init__json_trans_keys_0() {
        return new char[]{'\r', ' ', '\"', ',', '/', ':', '[', ']', '{', '\t', '\n', '*', '/', '\"', '*', '/', '\r', ' ', '\"', ',', '/', ':', '}', '\t', '\n', '\r', ' ', '/', ':', '\t', '\n', '\r', ' ', '/', ':', '\t', '\n', '*', '/', '\r', ' ', '\"', ',', '/', ':', '[', ']', '{', '\t', '\n', '\t', '\n', '\r', ' ', ',', '/', '}', '\t', '\n', '\r', ' ', ',', '/', '}', '\r', ' ', '\"', ',', '/', ':', '}', '\t', '\n', '\"', '\r', ' ', '\"', ',', '/', ':', '}', '\t', '\n', '*', '/', '*', '/', '\r', ' ', '\"', ',', '/', ':', '}', '\t', '\n', '*', '/', '*', '/', '\"', '*', '/', '*', '/', '\r', ' ', '\"', ',', '/', ':', '[', ']', '{', '\t', '\n', '\t', '\n', '\r', ' ', ',', '/', ']', '\t', '\n', '\r', ' ', ',', '/', ']', '\r', ' ', '\"', ',', '/', ':', '[', ']', '{', '\t', '\n', '\"', '\r', ' ', '\"', ',', '/', ':', '[', ']', '{', '\t', '\n', '*', '/', '*', '/', '\r', ' ', '\"', ',', '/', ':', '[', ']', '{', '\t', '\n', '*', '/', '*', '/', '*', '/', '\r', ' ', '/', '\t', '\n', '\r', ' ', '/', '\t', '\n', '\u0000'};
    }

    private static byte[] init__json_single_lengths_0() {
        return new byte[]{0, 9, 2, 1, 2, 7, 4, 4, 2, 9, 7, 7, 7, 1, 7, 2, 2, 7, 2, 2, 1, 2, 2, 9, 7, 7, 9, 1, 9, 2, 2, 9, 2, 2, 2, 3, 3, 0, 0};
    }

    private static byte[] init__json_range_lengths_0() {
        return new byte[]{0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0};
    }

    private static short[] init__json_index_offsets_0() {
        return new short[]{0, 0, 11, 14, 16, 19, 28, 34, 40, 43, 54, 62, 70, 79, 81, 90, 93, 96, 105, 108, 111, 113, 116, 119, 130, 138, 146, 157, 159, 170, 173, 176, 187, 190, 193, 196, 201, 206, 207};
    }

    private static byte[] init__json_indicies_0() {
        return new byte[]{1, 1, 2, 3, 4, 3, 5, 3, 6, 1, 0, 7, 7, 3, 8, 3, 9, 9, 3, 11, 11, 12, 13, 14, 3, 15, 11, 10, 16, 16, 17, 18, 16, 3, 19, 19, 20, 21, 19, 3, 22, 22, 3, 21, 21, 24, 3, 25, 3, 26, 3, 27, 21, 23, 28, 29, 28, 28, 30, 31, 32, 3, 33, 34, 33, 33, 13, 35, 15, 3, 34, 34, 12, 36, 37, 3, 15, 34, 10, 16, 3, 36, 36, 12, 3, 38, 3, 3, 36, 10, 39, 39, 3, 40, 40, 3, 13, 13, 12, 3, 41, 3, 15, 13, 10, 42, 42, 3, 43, 43, 3, 28, 3, 44, 44, 3, 45, 45, 3, 47, 47, 48, 49, 50, 3, 51, 52, 53, 47, 46, 54, 55, 54, 54, 56, 57, 58, 3, 59, 60, 59, 59, 49, 61, 52, 3, 60, 60, 48, 62, 63, 3, 51, 52, 53, 60, 46, 54, 3, 62, 62, 48, 3, 64, 3, 51, 3, 53, 62, 46, 65, 65, 3, 66, 66, 3, 49, 49, 48, 3, 67, 3, 51, 52, 53, 49, 46, 68, 68, 3, 69, 69, 3, 70, 70, 3, 8, 8, 71, 8, 3, 72, 72, 73, 72, 3, 3, 3, 0};
    }

    private static byte[] init__json_trans_targs_0() {
        return new byte[]{35, 1, 3, 0, 4, 36, 36, 36, 36, 1, 6, 5, 13, 17, 22, 37, 7, 8, 9, 7, 8, 9, 7, 10, 20, 21, 11, 11, 11, 12, 17, 19, 37, 11, 12, 19, 14, 16, 15, 14, 12, 18, 17, 11, 9, 5, 24, 23, 27, 31, 34, 25, 38, 25, 25, 26, 31, 33, 38, 25, 26, 33, 28, 30, 29, 28, 26, 32, 31, 25, 23, 2, 36, 2};
    }

    private static byte[] init__json_trans_actions_0() {
        return new byte[]{13, 0, 15, 0, 0, 7, 3, 11, 1, 11, 17, 0, 20, 0, 0, 5, 1, 1, 1, 0, 0, 0, 11, 13, 15, 0, 7, 3, 1, 1, 1, 1, 23, 0, 0, 0, 0, 0, 0, 11, 11, 0, 11, 11, 11, 11, 13, 0, 15, 0, 0, 7, 9, 3, 1, 1, 1, 1, 26, 0, 0, 0, 0, 0, 0, 11, 11, 0, 11, 11, 11, 1, 0, 0};
    }

    private static byte[] init__json_eof_actions_0() {
        return new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0};
    }

    private void addChild(String name, JsonValue child) {
        child.setName(name);
        if (this.current == null) {
            this.current = child;
            this.root = child;
        } else if (this.current.isArray() || this.current.isObject()) {
            child.parent = this.current;
            if (this.current.size == 0) {
                this.current.child = child;
            } else {
                JsonValue last = this.lastChild.pop();
                last.next = child;
                child.prev = last;
            }
            this.lastChild.add(child);
            ++this.current.size;
        } else {
            this.root = this.current;
        }
    }

    protected void startObject(String name) {
        JsonValue value = new JsonValue(JsonValue.ValueType.object);
        if (this.current != null) {
            this.addChild(name, value);
        }
        this.elements.add(value);
        this.current = value;
    }

    protected void startArray(String name) {
        JsonValue value = new JsonValue(JsonValue.ValueType.array);
        if (this.current != null) {
            this.addChild(name, value);
        }
        this.elements.add(value);
        this.current = value;
    }

    protected void pop() {
        this.root = this.elements.pop();
        if (this.current.size > 0) {
            this.lastChild.pop();
        }
        this.current = this.elements.size > 0 ? this.elements.peek() : null;
    }

    protected void string(String name, String value) {
        this.addChild(name, new JsonValue(value));
    }

    protected void number(String name, double value, String stringValue) {
        this.addChild(name, new JsonValue(value, stringValue));
    }

    protected void number(String name, long value, String stringValue) {
        this.addChild(name, new JsonValue(value, stringValue));
    }

    protected void bool(String name, boolean value) {
        this.addChild(name, new JsonValue(value));
    }

    private String unescape(String value) {
        int length = value.length();
        StringBuilder buffer = new StringBuilder(length + 16);
        int i = 0;
        while (i < length) {
            char c;
            if ((c = value.charAt(i++)) != '\\') {
                buffer.append(c);
                continue;
            }
            if (i == length) break;
            if ((c = value.charAt(i++)) == 'u') {
                buffer.append(Character.toChars(Integer.parseInt(value.substring(i, i + 4), 16)));
                i += 4;
                continue;
            }
            switch (c) {
                case '\"': 
                case '/': 
                case '\\': {
                    break;
                }
                case 'b': {
                    c = '\b';
                    break;
                }
                case 'f': {
                    c = '\f';
                    break;
                }
                case 'n': {
                    c = '\n';
                    break;
                }
                case 'r': {
                    c = '\r';
                    break;
                }
                case 't': {
                    c = '\t';
                    break;
                }
                default: {
                    throw new SerializationException("Illegal escaped character: \\" + c);
                }
            }
            buffer.append(c);
        }
        return buffer.toString();
    }
}

