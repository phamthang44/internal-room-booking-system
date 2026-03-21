package com.thang.roombooking.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity<T extends Serializable> {

    public abstract T getId();

}

//    protected void onPrePersist() {
//        // Hook for subclasses, ko làm gì ở đây ai cần thì override
//    }
//    @PrePersist
//    private void prePersist() {
//        onPrePersist();
//    }
// quan tâm ở đây là class BaseEntity chỉ nên là khung tranh Identity Contract
// không nên chứa logic gì khác ngoài việc định nghĩa phương thức trừu tượng getId
// vai trò thuần túy của nó là định nghĩa mọi Entity đều phải có 1 cái ID. Hết
// ko cần quan tâm tạo lúc nào vì điều này là của Audit rồi
// nếu cho nhiều điều vào đây nó sẽ bị biến thành God Class biết quá nhiều việc
// làm loãng đi mục đích chính của nó. -> Tư duy thiết kế Seperation of Concerns (SoC)
