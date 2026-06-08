/**
 * TaskFlow 主题系统
 * 支持 8 套配色方案，通过 CSS 变量 + localStorage 持久化
 * 每种主题包含 6 个分类颜色，分类名称固定映射到颜色索引
 */

// 分类 → 颜色索引 映射（固定）
const CATEGORY_COLOR_MAP = {
    '学习': 0,
    '运动': 1,
    '生活': 2,
    '阅读': 3,
    '其他': 4
};
// 未匹配的分类使用索引 5
const DEFAULT_COLOR_IDX = 5;

const THEMES = {
    blue: {
        name: '莫兰迪蓝色系',
        vars: {
            '--primary': '#73A8C2', '--primary-dark': '#5A8FA8', '--primary-light': '#E4F0F6',
            '--bg': '#EEF3F7', '--card': '#FFFFFF', '--text': '#2C3E50', '--text-light': '#7F8C8D',
            '--border': '#E8ECF1', '--success': '#6ECB8C', '--warning': '#F5A623', '--danger': '#E85D75'
        },
        // 分类颜色: 学习, 运动, 生活, 阅读, 其他, 自定义
        catColors: ['#5A8FA8', '#4E8DA0', '#689DB2', '#7BAFC4', '#5090A6', '#86B8CA']
    },
    green: {
        name: '莫兰迪绿色系',
        vars: {
            '--primary': '#6B9E85', '--primary-dark': '#4F7D66', '--primary-light': '#E0EFE7',
            '--bg': '#EDF4F0', '--card': '#FFFFFF', '--text': '#2C3E50', '--text-light': '#7F8C8D',
            '--border': '#D8E8DE', '--success': '#5EA873', '--warning': '#D4952A', '--danger': '#D4726A'
        },
        catColors: ['#4F7D66', '#3D6A54', '#62917A', '#6E9A84', '#53886E', '#7CA892']
    },
    purple: {
        name: '莫兰迪紫色系',
        vars: {
            '--primary': '#9B8EC4', '--primary-dark': '#7B6EA6', '--primary-light': '#EDE8F7',
            '--bg': '#F4F2F8', '--card': '#FFFFFF', '--text': '#3A3450', '--text-light': '#8A84A0',
            '--border': '#E0DBEE', '--success': '#7EB894', '--warning': '#D4A45A', '--danger': '#D47A7A'
        },
        catColors: ['#7B6EA6', '#6d5e9b', '#8E82B6', '#9C90C2', '#8274AA', '#A89CC8']
    },
    macaron: {
        name: '马卡龙色系',
        vars: {
            '--primary': '#8BC4BA', '--primary-dark': '#6DA89E', '--primary-light': '#E8F7F4',
            '--bg': '#F5FAF8', '--card': '#FFFFFF', '--text': '#4A5568', '--text-light': '#8E9BAE',
            '--border': '#E2EDEA', '--success': '#90C9A7', '--warning': '#F0C78E', '--danger': '#F0A8A8'
        },
        catColors: ['#8BC4BA', '#A8D5C8', '#F0C78E', '#C8E0D8', '#F0A8A8', '#D8B8E0']
    },
    candy: {
        name: '糖果色系',
        vars: {
            '--primary': '#F097A0', '--primary-dark': '#E07A85', '--primary-light': '#FDE8EB',
            '--bg': '#FFF5F5', '--card': '#FFFFFF', '--text': '#5A3E42', '--text-light': '#A89094',
            '--border': '#F5DEE2', '--success': '#8ECB9E', '--warning': '#F5C063', '--danger': '#E8747A'
        },
        catColors: ['#E07A85', '#F5A0B0', '#F5C063', '#F8A8B8', '#F0C8D8', '#E8B8D0']
    },
    white: {
        name: '国风白色系',
        vars: {
            '--primary': '#B0A89A', '--primary-dark': '#8F877A', '--primary-light': '#F0EDE8',
            '--bg': '#F8F6F3', '--card': '#FFFFFF', '--text': '#4A4540', '--text-light': '#9A9490',
            '--border': '#E8E4DE', '--success': '#9CB8A0', '--warning': '#C9A96E', '--danger': '#C98B84'
        },
        catColors: ['#8F877A', '#A09080', '#B0A090', '#988878', '#B8A898', '#C0B0A0']
    },
    pink: {
        name: '国风粉色系',
        vars: {
            '--primary': '#D4A0AE', '--primary-dark': '#C08090', '--primary-light': '#F8EEF1',
            '--bg': '#FBF6F7', '--card': '#FFFFFF', '--text': '#5A3E44', '--text-light': '#A89096',
            '--border': '#F0DEE3', '--success': '#A8C9B0', '--warning': '#D4B878', '--danger': '#D4888E'
        },
        catColors: ['#C08090', '#D4A0AE', '#B87080', '#E0B6C4', '#C898A8', '#E8C6D2']
    },
    yellow: {
        name: '国风黄色系',
        vars: {
            '--primary': '#D4C878', '--primary-dark': '#B8A85A', '--primary-light': '#F8F3E0',
            '--bg': '#FBFAF0', '--card': '#FFFFFF', '--text': '#5A5230', '--text-light': '#A09870',
            '--border': '#ECE4C0', '--success': '#98C8A0', '--warning': '#D4A040', '--danger': '#D48880'
        },
        catColors: ['#B8A85A', '#C8B870', '#A89848', '#D8C878', '#C0B068', '#E0D088']
    }
};

function getCurrentTheme() {
    return localStorage.getItem('appTheme') || 'blue';
}

function applyTheme(themeName) {
    const theme = THEMES[themeName];
    if (!theme) return;
    const root = document.documentElement;
    Object.entries(theme.vars).forEach(([key, value]) => {
        root.style.setProperty(key, value);
    });
    root.style.setProperty('--cat-study',    theme.catColors[0]);
    root.style.setProperty('--cat-exercise', theme.catColors[1]);
    root.style.setProperty('--cat-life',     theme.catColors[2]);
    root.style.setProperty('--cat-reading',  theme.catColors[3]);
    root.style.setProperty('--cat-other',    theme.catColors[4]);
    root.style.setProperty('--cat-custom',   theme.catColors[5]);
    localStorage.setItem('appTheme', themeName);
}

/** 获取全部6个分类颜色（用于循环渲染） */
function getCategoryColors() {
    const theme = THEMES[getCurrentTheme()];
    return theme ? theme.catColors : THEMES.blue.catColors;
}

/** 根据分类名称获取对应颜色 */
function getCategoryColor(categoryName) {
    const colors = getCategoryColors();
    const idx = CATEGORY_COLOR_MAP[categoryName];
    return idx !== undefined ? colors[idx] : colors[DEFAULT_COLOR_IDX];
}

(function() {
    applyTheme(getCurrentTheme());
})();
