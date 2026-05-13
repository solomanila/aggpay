export const AREA_TYPE_MAP = {
  1:  { label: '国内',     currency: '¥',  currencyCode: 'CNY' },
  2:  { label: '印度',     currency: '₹',  currencyCode: 'INR' },
  4:  { label: '印尼',     currency: 'Rp', currencyCode: 'IDR' },
  5:  { label: '非洲',     currency: 'Fr', currencyCode: 'XOF' },
  6:  { label: '泰国',     currency: '฿',  currencyCode: 'THB' },
  7:  { label: '墨西哥',   currency: '$',  currencyCode: 'MXN' },
  8:  { label: '巴西',     currency: 'R$', currencyCode: 'BRL' },
  9:  { label: '巴基斯坦', currency: '₨',  currencyCode: 'PKR' },
  10: { label: '孟加拉国', currency: '৳',  currencyCode: 'BDT' },
  11: { label: '日本',     currency: '¥',  currencyCode: 'JPY' },
  12: { label: '俄罗斯',   currency: '₽',  currencyCode: 'RUB' },
  13: { label: '马来西亚', currency: 'RM', currencyCode: 'MYR' },
  14: { label: '埃及',     currency: 'E£', currencyCode: 'EGP' },
}

export function areaLabel(areaType) {
  return AREA_TYPE_MAP[areaType]?.label ?? null
}

export function areaCurrency(areaType) {
  return AREA_TYPE_MAP[areaType]?.currency ?? null
}

export function areaCurrencyCode(areaType) {
  return AREA_TYPE_MAP[areaType]?.currencyCode ?? null
}
