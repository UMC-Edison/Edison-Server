package com.edison.project.domain.label.entity;

import com.edison.project.domain.member.entity.Member;
import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Label extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long labelId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private LabelColor color;

    @ManyToOne
    @JoinColumn(name = "member_id") // 로그인 안한 유저 id값 없다면, nullable=true 추가
    private Member member;

    public enum LabelColor {
        PINK400("#FF47AD"),
        PINK300("#FF6CBF"),
        PINK100("#FBC1E5"),
        RED500("#F44336"),
        RED300("#E57373"),
        RED100("#FBCDD2"),
        YELLOW500("#FBC000"),
        YELLOW300("#FFDC3A"),
        YELLOW100("#FFF0AB"),
        GREEN500("#00CC74"),
        GREEN300("#5BDE9F"),
        GREEN100("#BFF0D5"),
        AQUA500("#00A2EE"),
        AQUA300("#4CBEF3"),
        AQUA100("#B2E3FA"),
        INDIGO400("#5C6CC0"),
        INDIGO300("#798BCB"),
        INDIGO100("#C5CAE9"),
        PURPLE500("#9632AF"),
        PURPLE300("#B56CC8"),
        PURPLE100("#DFBFE7");

        private final String hexCode;

        LabelColor(String hexCode) {
            this.hexCode = hexCode;
        }

        public String getHexCode() {
            return hexCode;
        }
    }

}
