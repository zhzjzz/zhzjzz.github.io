const ICON_RULES = [
  { icon: 'CoffeeMachine', label: '咖啡', keywords: ['咖啡', 'coffee', 'cafe', 'latte'] },
  { icon: 'Tea', label: '饮品', keywords: ['饮品', '茶', 'tea', 'beverage', 'drink', '奶茶'] },
  { icon: 'Bread', label: '烘焙', keywords: ['烘焙', '面包', 'bakery', 'bread'] },
  { icon: 'Cake', label: '甜品', keywords: ['甜品', '蛋糕', 'dessert', 'cake', 'ice_cream', 'confectionery'] },
  { icon: 'Hamburger', label: '快餐', keywords: ['快餐', '西式简餐', 'fast_food', 'burger', 'hamburger', 'sandwich', 'american'] },
  { icon: 'Noodles', label: '面食', keywords: ['面食', '面', 'noodle', 'noodles', 'ramen'] },
  { icon: 'ChickenLeg', label: '炸鸡', keywords: ['鸡', 'chicken', '炸鸡'] },
  { icon: 'Cup', label: '火锅', keywords: ['火锅', 'hotpot'] },
  { icon: 'ForkSpoon', label: '西餐', keywords: ['西餐', 'italian', 'french', 'western', 'pizza', 'german'] },
  { icon: 'KnifeFork', label: '烧烤', keywords: ['烧烤', 'barbecue', 'bbq', '烤'] },
]

const FALLBACK = {
  icon: 'ChopsticksFork',
  label: '餐饮',
}

const normalize = (value) => String(value || '').trim().toLowerCase()

const searchText = (food = {}) => [
  food.cuisine,
  food.name,
  food.storeName,
].map(normalize).filter(Boolean).join(' ')

export const foodIconRule = (food = {}) => {
  const text = searchText(food)
  return ICON_RULES.find((rule) => rule.keywords.some((keyword) => text.includes(keyword.toLowerCase()))) || FALLBACK
}

export const foodIconName = (food = {}) => foodIconRule(food).icon

export const foodIconLabel = (food = {}) => foodIconRule(food).label
