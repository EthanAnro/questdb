/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.test.griffin.engine.functions.regex;

import io.questdb.cairo.sql.RecordCursor;
import io.questdb.cairo.sql.RecordCursorFactory;
import io.questdb.std.Chars;
import io.questdb.test.AbstractCairoTest;
import org.junit.Assert;
import org.junit.Test;

public class ILikeVarcharFunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testBindVariableConcatIndexed() throws Exception {
        assertMemoryLeak(() -> {
            ddl("create table x as (select rnd_varchar() name from long_sequence(2000))");

            bindVariableService.setStr(0, "H");
            try (RecordCursorFactory factory = select("select * from x where name ilike '%' || $1 || '%'")) {
                try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                    println(factory, cursor);
                    Assert.assertNotEquals(sink.toString().indexOf('H'), -1);
                }
            }
        });
    }

    @Test
    public void testBindVariableConcatNamed() throws Exception {
        assertMemoryLeak(() -> {
            ddl("create table x as (select rnd_varchar() name from long_sequence(2000))");

            bindVariableService.setStr("str", "H");
            try (RecordCursorFactory factory = select("select * from x where name ilike '%' || :str || '%'")) {
                try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                    println(factory, cursor);
                    Assert.assertNotEquals(sink.toString().indexOf('H'), -1);
                }
            }
        });
    }

    @Test
    public void testEmptyLike() throws Exception {
        assertMemoryLeak(() -> {
            ddl(
                    "create table x as (\n" +
                            "select cast('ABCGE' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('SBDHDJ' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('BDGDGGG' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('AAAAVVV' as varchar) as name from long_sequence(1)\n" +
                            ")"
            );
            assertSql(
                    "name\n",
                    "select * from x where name ilike ''"
            );
        });
    }

    @Test
    public void testInvalidRegex() throws Exception {
        assertMemoryLeak(() -> {
            ddl(
                    "create table x as (\n" +
                            "select cast('ABCGE' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('SBDHDJ' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('BDGDGGG' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('AAAAVVV' as varchar) as name from long_sequence(1)\n" +
                            ")"
            );
            assertSql(
                    "name\n",
                    "select * from x where name ilike '[][n'"
            );
        });
    }

    @Test
    public void testLikeCaseInsensitive() throws Exception {
        assertMemoryLeak(() -> {
            ddl(
                    "create table x as (\n" +
                            "select cast('ABCGE' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('SBDHDJ' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('BDGDGGG' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('AAAAVVV' as varchar) as name from long_sequence(1)\n" +
                            ")"
            );
            assertSql(
                    "name\n" +
                            "ABCGE\n",
                    "select * from x where name ilike 'aBcGe'"
            );
        });
    }

    @Test
    public void testLikeCaseInsensitiveNonAscii() throws Exception {
        assertMemoryLeak(() -> {
            ddl(
                    "create table x as (\n" +
                            "select cast('фу' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('бар' as varchar) as name from long_sequence(1)\n" +
                            "union\n" +
                            "select cast('баз' as varchar) as name from long_sequence(1)\n" +
                            ")"
            );
            assertSql(
                    "name\n" +
                            "бар\n",
                    "select * from x where name ilike 'БаР'"
            );
        });
    }

    @Test
    public void testLikeCharacterNoMatch() throws Exception {
        assertMemoryLeak(() -> {
            ddl("create table x as (select rnd_varchar() name from long_sequence(2000))");
            try (RecordCursorFactory factory = select("select * from x where name ilike 'H'")) {
                try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                    println(factory, cursor);
                    Assert.assertEquals(Chars.indexOf(sink, 'H'), -1);
                }
            }
        });
    }

    @Test
    public void testNonConstantExpression() throws Exception {
        assertMemoryLeak(() -> {
            ddl("create table x as (select rnd_varchar() name from long_sequence(10))");
            assertException("select * from x where name ilike rnd_str('foo','bar')", 33, "use constant or bind variable");
        });
    }

    @Test
    public void testNotLikeCharacterMatch() throws Exception {
        assertMemoryLeak(() -> {
            ddl("create table x as (select rnd_varchar('a', 'BC', 'h', 'H', 'k') name from long_sequence(20))");
            assertSql(
                    "name\n" +
                            "a\n" +
                            "BC\n" +
                            "BC\n" +
                            "k\n" +
                            "BC\n" +
                            "BC\n" +
                            "BC\n" +
                            "k\n" +
                            "BC\n" +
                            "BC\n" +
                            "a\n" +
                            "k\n",
                    "select * from x where not name ilike 'H'"
            );
        });
    }

    @Test
    public void testNotLikeStringMatch() throws Exception {
        assertMemoryLeak(() -> {
            ddl("create table x as (select rnd_varchar('kk', 'xJ', 'Xj', 'GU', 'XJ') name from long_sequence(20))");
            assertSql(
                    "name\n" +
                            "kk\n" +
                            "GU\n" +
                            "GU\n" +
                            "GU\n" +
                            "GU\n" +
                            "kk\n",
                    "select * from x where not name ilike 'XJ'"
            );
        });
    }

    @Test
    public void testSimplePatternLike() throws Exception {
        assertMemoryLeak(() -> {
            compile("create table x ( s varchar ) ");
            compile("insert into x values ( 'foo' ), ( 'foobar' ), ( null ) ");

            assertLike("s\n", "select * from x where s ilike 'f'", false);
            assertLike("s\n", "select * from x where s ilike '_'", false);
            assertLike("s\nfoo\nfoobar\n", "select * from x where s ilike '%'", false);
            assertLike("s\nfoo\nfoobar\n", "select * from x where s ilike 'f%'", false);
            assertLike("s\nfoobar\n", "select * from x where s ilike '%r'", false);
            assertLike("s\nfoo\nfoobar\n", "select * from x where s ilike 'fOO%'", false);
            assertLike("s\nfoobar\n", "select * from x where s ilike '%baR'", false);
            assertLike("s\nfoo\nfoobar\n", "select * from x where s ilike '%%'", false);
            assertLike("s\nfoo\nfoobar\n", "select * from x where s ilike '%OO%'", false);
            assertLike("s\n", "select * from x where s ilike '%\\%'", false);
        });
    }

    @Test
    public void testSimplePatternLikeNonAscii() throws Exception {
        assertMemoryLeak(() -> {
            compile("create table x ( s varchar ) ");
            compile("insert into x values ( 'фу' ), ( 'фубар' ), ( null ) ");

            assertLike("s\n", "select * from x where s ilike 'ф'", false);
            assertLike("s\n", "select * from x where s ilike '_'", false);
            assertLike("s\nфу\nфубар\n", "select * from x where s ilike '%'", false);
            assertLike("s\nфу\nфубар\n", "select * from x where s ilike 'ф%'", false);
            assertLike("s\nфубар\n", "select * from x where s ilike '%р'", false);
            assertLike("s\nфу\nфубар\n", "select * from x where s ilike 'фУ%'", false);
            assertLike("s\nфу\n", "select * from x where s ilike 'фУ'", false);
            assertLike("s\nфубар\n", "select * from x where s ilike '%баР'", false);
            assertLike("s\nфу\nфубар\n", "select * from x where s ilike '%У%'", false);
            assertLike("s\n", "select * from x where s ilike '\\_'", false);
        });
    }

    private void assertLike(String expected, String query, boolean expectSize) throws Exception {
        assertQueryNoLeakCheck(expected, query, null, true, expectSize);
    }
}
