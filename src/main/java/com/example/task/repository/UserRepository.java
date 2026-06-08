//仓库接口
package com.example.task.repository;

import com.example.task.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//JpaRepository<User, Long>: 这是 Spring Data JPA 提供的一个强大接口。
//只要继承了这个接口，你不需要写任何 SQL 语句，就已经自动拥有了 save() (保存/修改), 
// findById() (按ID查询), findAll() (查询全部), deleteById() (按ID删除) 等方法。
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}