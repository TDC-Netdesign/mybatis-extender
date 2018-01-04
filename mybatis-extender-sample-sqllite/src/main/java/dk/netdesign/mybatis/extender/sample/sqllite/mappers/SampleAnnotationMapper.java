package dk.netdesign.mybatis.extender.sample.sqllite.mappers;

import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by nmw on 26-04-2017.
 */
public interface SampleAnnotationMapper {

    @Select("SELECT datetime('now')")
    public String sayHello();

    @Select("SELECT datetime('now')\n" +
            "UNION SELECT 1\n" +
            "UNION SELECT 2\n" +
            "UNION SELECT 3\n" +
            "UNION SELECT 4\n" +
            "UNION SELECT 5\n" +
            "UNION SELECT 6")
    public List<String> getAllTables();

}
