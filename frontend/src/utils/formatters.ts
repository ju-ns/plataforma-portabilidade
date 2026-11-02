const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

export function formatCurrency(value: number): string {
  return currencyFormatter.format(value);
}

export function formatDate(value: string | null): string {
  if (!value) {
    return "Não informada";
  }

  const [year, month, day] = value.split("-");

  return `${day}/${month}/${year}`;
}